package com.error404.dominoapp.ui.history

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.error404.dominoapp.R
import com.error404.dominoapp.data.local.DominoDatabase
import com.error404.dominoapp.data.local.entity.Mode
import com.error404.dominoapp.data.repository.MatchRepository
import com.error404.dominoapp.ui.str
import com.error404.dominoapp.ui.theme.DominoAppTheme
import com.error404.dominoapp.ui.waitForNoText
import com.error404.dominoapp.ui.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The history list, with its real ViewModel and an in-memory database. */
@RunWith(AndroidJUnit4::class)
class HistoryScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var db: DominoDatabase
    private lateinit var repository: MatchRepository
    private var openedMatchId: Long? = null

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DominoDatabase::class.java)
            .build()
        repository = MatchRepository(db)
    }

    @After
    fun tearDown() = db.close()

    private fun showHistory() {
        val viewModel = HistoryViewModel(repository)
        rule.setContent {
            DominoAppTheme {
                HistoryScreen(onBack = {}, onOpenMatch = { openedMatchId = it }, viewModel = viewModel)
            }
        }
    }

    /** "Nosotros" beats "Ellos" 105–30 in a match to 100. Returns its id. */
    private fun seedWonMatch(): Long = runBlocking {
        val id = repository.startMatch(Mode.PAIRS, listOf("Nosotros", "Ellos"), targetScore = 100)
        val (us, them) = db.matchDao().getParticipants(id).map { it.id }
        repository.saveHand(id, mapOf(them to 30))
        repository.saveHand(id, mapOf(us to 105))
        id
    }

    private fun deleteButton(names: String) =
        rule.onNodeWithContentDescription(str(R.string.delete_match_cd, names))

    @Test
    fun noMatches_showsTheEmptyMessage() {
        showHistory()
        rule.waitForText(str(R.string.history_empty))
    }

    @Test
    fun wonMatch_showsScoresWinnerAndFooter() {
        seedWonMatch()
        showHistory()

        rule.waitForText("Nosotros")
        rule.onNodeWithText("105").assertExists()
        rule.onNodeWithText("30").assertExists()
        rule.onNodeWithContentDescription(str(R.string.winner_cd)).assertExists()
        rule.onNodeWithText(str(R.string.history_footer, 100, 2)).assertExists()
        rule.onNodeWithText(str(R.string.status_in_progress)).assertDoesNotExist()
    }

    @Test
    fun inProgressAndAbandonedMatches_areTagged() {
        runBlocking {
            repository.startMatch(Mode.PAIRS, listOf("A", "B"), 200) // abandoned by the next one
            repository.startMatch(Mode.PAIRS, listOf("C", "D"), 200) // in progress
        }
        showHistory()

        rule.waitForText(str(R.string.status_in_progress))
        rule.onNodeWithText(str(R.string.status_abandoned)).assertExists()
    }

    @Test
    fun tappingAMatch_opensIt() {
        val id = seedWonMatch()
        showHistory()

        rule.waitForText("Nosotros")
        rule.onNodeWithText("Nosotros").performClick()

        assertEquals(id, openedMatchId)
    }

    @Test
    fun delete_asksFirst_andCancelKeepsTheMatch() {
        seedWonMatch()
        showHistory()
        rule.waitForText("Nosotros")

        deleteButton("Nosotros vs Ellos").performClick()
        rule.onNodeWithText(str(R.string.delete_match_title)).assertExists()
        rule.onNodeWithText(str(R.string.cancel)).performClick()

        rule.onNodeWithText(str(R.string.delete_match_title)).assertDoesNotExist()
        rule.onNodeWithText("Nosotros").assertExists()
    }

    @Test
    fun delete_confirmed_removesTheMatch() {
        seedWonMatch()
        showHistory()
        rule.waitForText("Nosotros")

        deleteButton("Nosotros vs Ellos").performClick()
        rule.onNodeWithText(str(R.string.delete)).performClick()

        rule.waitForNoText("Nosotros")
        rule.waitForText(str(R.string.history_empty))
    }
}
