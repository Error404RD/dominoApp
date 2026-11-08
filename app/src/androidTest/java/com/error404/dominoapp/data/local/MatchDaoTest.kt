package com.error404.dominoapp.data.local

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.error404.dominoapp.data.local.entity.HandEntity
import com.error404.dominoapp.data.local.entity.HandScoreEntity
import com.error404.dominoapp.data.local.entity.MatchEntity
import com.error404.dominoapp.data.local.entity.Mode
import com.error404.dominoapp.data.local.entity.ParticipantEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchDaoTest {

    private lateinit var db: DominoDatabase
    private lateinit var dao: MatchDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DominoDatabase::class.java,
        ).build()
        dao = db.matchDao()
    }

    @After
    fun tearDown() = db.close()

    /** Creates a match with the given participant names and returns (matchId, participantIds). */
    private suspend fun createMatch(vararg names: String): Pair<Long, List<Long>> {
        val matchId = dao.insertMatch(MatchEntity(mode = Mode.INDIVIDUAL, targetScore = 200, startedAt = 0))
        val ids = dao.insertParticipants(
            names.mapIndexed { i, name -> ParticipantEntity(matchId = matchId, name = name, position = i) },
        )
        return matchId to ids
    }

    private suspend fun addHand(matchId: Long, vararg scores: Pair<Long, Int>): Long {
        val handId = dao.insertHand(
            HandEntity(matchId = matchId, number = dao.nextHandNumber(matchId), playedAt = 0),
        )
        dao.insertScores(scores.map { (pid, pts) -> HandScoreEntity(handId = handId, participantId = pid, points = pts) })
        return handId
    }

    @Test
    fun totalsSumAllHandsAndIncludeParticipantsWithoutPoints() = runTest {
        val (matchId, ids) = createMatch("Ana", "Luis", "Eva")
        addHand(matchId, ids[0] to 30, ids[1] to 10)
        addHand(matchId, ids[0] to 25)

        val totals = dao.getTotals(matchId)

        assertEquals(listOf("Ana", "Luis", "Eva"), totals.map { it.name })
        assertEquals(listOf(55, 10, 0), totals.map { it.total })
    }

    @Test
    fun handNumbersIncrease() = runTest {
        val (matchId, ids) = createMatch("A", "B")
        addHand(matchId, ids[0] to 1)
        addHand(matchId, ids[0] to 1)

        assertEquals(listOf(1, 2), dao.observeHands(matchId).first().map { it.hand.number })
    }

    @Test
    fun deletingHandRemovesItsScores() = runTest {
        val (matchId, ids) = createMatch("A", "B")
        val handId = addHand(matchId, ids[0] to 40)
        addHand(matchId, ids[0] to 5)

        dao.deleteHand(handId)

        assertEquals(5, dao.getTotals(matchId).first().total)
    }

    @Test
    fun deletingMatchCascadesToEverything() = runTest {
        val (matchId, ids) = createMatch("A", "B")
        val handId = addHand(matchId, ids[0] to 40)

        dao.deleteMatch(matchId)

        assertNull(dao.getMatch(matchId))
        assertNull(dao.getHand(handId))
        assertTrue(dao.getTotals(matchId).isEmpty())
    }

    @Test
    fun inProgressAndHistoryAreSplitByFinishedAt() = runTest {
        val (finishedId, _) = createMatch("A", "B")
        dao.updateMatch(dao.getMatch(finishedId)!!.copy(finishedAt = 10))
        val (openId, _) = createMatch("C", "D")

        assertEquals(openId, dao.observeMatchInProgress().first()?.match?.id)
        assertEquals(listOf(finishedId), dao.observeHistory().first().map { it.match.id })
    }
}
