package com.error404.dominoapp.data.repository

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.error404.dominoapp.data.local.DominoDatabase
import com.error404.dominoapp.data.local.entity.Mode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MatchRepositoryTest {

    private lateinit var db: DominoDatabase
    private lateinit var repo: MatchRepository
    private var clock = 1_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DominoDatabase::class.java,
        ).build()
        repo = MatchRepository(db, now = { clock++ })
    }

    @After
    fun tearDown() = db.close()

    private suspend fun participantIds(matchId: Long) = db.matchDao().getParticipants(matchId).map { it.id }

    @Test
    fun playingUntilTarget_declaresWinnerAndFinishesMatch() = runTest {
        val matchId = repo.startMatch(Mode.PAIRS, listOf("Nosotros", "Ellos"), targetScore = 100)
        val (us, them) = participantIds(matchId)

        assertNull(repo.saveHand(matchId, mapOf(us to 60, them to 30)))
        assertNull(repo.observeMatch(matchId).first()!!.finishedAt)

        assertEquals(us, repo.saveHand(matchId, mapOf(us to 45)))
        val match = repo.observeMatch(matchId).first()!!
        assertEquals(us, match.winnerId)
        assertNotNull(match.finishedAt)
        assertEquals(listOf(105, 30), repo.observeTotals(matchId).first().map { it.total })
    }

    @Test
    fun editingWinningHandBelowTarget_reopensMatch() = runTest {
        val matchId = repo.startMatch(Mode.PAIRS, listOf("A", "B"), targetScore = 100)
        val (a, _) = participantIds(matchId)
        repo.saveHand(matchId, mapOf(a to 120))
        val handId = repo.observeHands(matchId).first().single().hand.id

        assertNull(repo.editHand(handId, mapOf(a to 20)))

        val match = repo.observeMatch(matchId).first()!!
        assertNull(match.winnerId)
        assertNull(match.finishedAt)
    }

    @Test
    fun deletingWinningHand_reopensMatch() = runTest {
        val matchId = repo.startMatch(Mode.INDIVIDUAL, listOf("A", "B", "C"), targetScore = 100)
        val (a, _, _) = participantIds(matchId)
        repo.saveHand(matchId, mapOf(a to 50))
        repo.saveHand(matchId, mapOf(a to 60))
        val lastHand = repo.observeHands(matchId).first().last().hand.id

        assertNull(repo.deleteHand(lastHand))
        assertNull(repo.observeMatch(matchId).first()!!.finishedAt)
        assertEquals(50, repo.observeTotals(matchId).first().first().total)
    }

    @Test
    fun startingNewMatch_abandonsTheOneInProgress() = runTest {
        val first = repo.startMatch(Mode.PAIRS, listOf("A", "B"), targetScore = 200)
        val second = repo.startMatch(Mode.PAIRS, listOf("C", "D"), targetScore = 200)

        assertTrue(repo.observeMatch(first).first()!!.isAbandoned)
        assertEquals(second, repo.observeMatchInProgress().first()?.match?.id)
        assertEquals(listOf(first), repo.observeHistory().first().map { it.match.id })
    }

    @Test
    fun missingParticipantsGetZero() = runTest {
        val matchId = repo.startMatch(Mode.INDIVIDUAL, listOf("A", "B", "C"), targetScore = 200)
        val (a, _, _) = participantIds(matchId)

        repo.saveHand(matchId, mapOf(a to 25))

        assertEquals(3, repo.observeHands(matchId).first().single().scores.size)
        assertEquals(listOf(25, 0, 0), repo.observeTotals(matchId).first().map { it.total })
    }

    @Test
    fun invalidInput_isRejected() = runTest {
        assertThrows { repo.startMatch(Mode.PAIRS, listOf("A", "B", "C"), 200) }
        assertThrows { repo.startMatch(Mode.INDIVIDUAL, listOf("A", " "), 200) }
        assertThrows { repo.startMatch(Mode.INDIVIDUAL, listOf("A", "B"), 0) }

        val matchId = repo.startMatch(Mode.PAIRS, listOf("A", "B"), 200)
        val (a, _) = participantIds(matchId)
        assertThrows { repo.saveHand(matchId, mapOf(a to -5)) }
        assertThrows { repo.saveHand(matchId, mapOf(9_999L to 5)) }
        assertTrue(repo.observeHands(matchId).first().isEmpty())
    }

    private suspend fun assertThrows(block: suspend () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // ok
        }
    }
}
