package com.error404.dominoapp.ui.scoreboard

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.error404.dominoapp.R
import com.error404.dominoapp.data.local.DominoDatabase
import com.error404.dominoapp.data.local.entity.Mode
import com.error404.dominoapp.data.repository.MatchRepository
import com.error404.dominoapp.ui.field
import com.error404.dominoapp.ui.str
import com.error404.dominoapp.ui.theme.DominoAppTheme
import com.error404.dominoapp.ui.waitForDescription
import com.error404.dominoapp.ui.waitForNoText
import com.error404.dominoapp.ui.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The scoreboard of a "Nosotros vs Ellos" match to 100, with an in-memory database. */
@RunWith(AndroidJUnit4::class)
class ScoreboardScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var db: DominoDatabase
    private var backClicks = 0
    private var newMatchClicks = 0

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DominoDatabase::class.java)
            .build()
        val repository = MatchRepository(db)
        val matchId = runBlocking { repository.startMatch(Mode.PAIRS, listOf("Nosotros", "Ellos"), targetScore = 100) }
        // The real app passes matchId through the navigation route; here we pass it directly.
        val viewModel = ScoreboardViewModel(SavedStateHandle(mapOf("matchId" to matchId)), repository)
        rule.setContent {
            DominoAppTheme {
                ScoreboardScreen(onBack = { backClicks++ }, onNewMatch = { newMatchClicks++ }, viewModel = viewModel)
            }
        }
        rule.waitForText("Nosotros")
    }

    @After
    fun tearDown() = db.close()

    // ---------- Helpers ----------

    private fun addButton(team: String) = rule.onNodeWithContentDescription(str(R.string.add_points_for, team))
    private val pointsField get() = rule.field(str(R.string.editor_points))
    private val winnerTitle = str(R.string.winner_title, "Nosotros")

    /** Taps "Anotar" on [team], types [points] and presses the keyboard's Done key. */
    private fun addHand(team: String, points: String) {
        addButton(team).performClick()
        pointsField.performTextInput(points)
        pointsField.performImeAction()
        rule.waitForNoText(str(R.string.editor_new_title))
    }

    private fun openHand(number: Int) {
        rule.onNodeWithText(str(R.string.hand_number, number)).performClick()
        rule.waitForText(str(R.string.editor_edit_title, number))
    }

    /** Nosotros 60 + 45 = 105 ≥ 100: wins with two hands. */
    private fun playUntilNosotrosWins() {
        addHand("Nosotros", "60")
        rule.waitForText(str(R.string.points_left, 40))
        addHand("Nosotros", "45")
        rule.waitForText(winnerTitle)
    }

    // ---------- Tests ----------

    @Test
    fun newMatch_showsBothTeamsAtZero_andTheEmptyHint() {
        rule.onNodeWithText(str(R.string.target_info, 100)).assertExists()
        assertEquals(2, rule.onAllNodesWithText(str(R.string.points_left, 100)).fetchSemanticsNodes().size)
        rule.onNodeWithText(str(R.string.no_hands)).assertExists()
    }

    @Test
    fun addingAHand_updatesTheTotalAndTheList() {
        addHand("Nosotros", "45")

        rule.waitForText(str(R.string.points_left, 55))
        rule.onNodeWithText("45").assertExists()
        rule.onNodeWithText("+45").assertExists()
        rule.onNodeWithText(str(R.string.hand_number, 1)).assertExists()
        rule.onNodeWithText(str(R.string.no_hands)).assertDoesNotExist()
    }

    @Test
    fun theSheet_canScoreForTheOtherTeam() {
        addButton("Nosotros").performClick()
        // The team options in the sheet are selectable; the score cards behind it are not.
        rule.onNode(hasText("Ellos") and isSelectable()).performClick()
        pointsField.performTextInput("20")
        pointsField.performImeAction()

        rule.waitForText(str(R.string.points_left, 80))
        rule.onNodeWithText(str(R.string.points_left, 100)).assertExists() // Nosotros unchanged
    }

    @Test
    fun saveButton_isDisabledUntilPointsAreValid() {
        addButton("Nosotros").performClick()
        val save = rule.onNodeWithText(str(R.string.save))

        save.assertIsNotEnabled()
        pointsField.performTextInput("0")
        save.assertIsNotEnabled() // 0 points is not a hand
        pointsField.performTextInput("5")
        save.assertIsEnabled()
    }

    @Test
    fun reachingTheTarget_announcesTheWinner_andHidesTheAddButtons() {
        playUntilNosotrosWins()

        rule.onNodeWithText(str(R.string.winner_subtitle, 105)).assertExists()
        addButton("Nosotros").assertDoesNotExist()
        addButton("Ellos").assertDoesNotExist()
    }

    @Test
    fun winnerBanner_newMatchButton_callsOnNewMatch() {
        playUntilNosotrosWins()
        rule.onNodeWithText(str(R.string.new_match_title)).performClick()
        assertEquals(1, newMatchClicks)
    }

    /**
     * Regression test: the edit field used to open with the cursor at the start,
     * so typing "10" over "45" produced "1045"/"145" instead of replacing it.
     */
    @Test
    fun editingAHand_replacesTheNumber_andReopensTheMatch() {
        playUntilNosotrosWins()

        openHand(2)
        pointsField.assertTextContains("45")
        pointsField.performTextInput("10")
        pointsField.assertTextContains("10")
        pointsField.performImeAction()

        // 60 + 10 = 70 < 100: the win is undone and the match continues.
        rule.waitForText(str(R.string.points_left, 30))
        rule.onNodeWithText(winnerTitle).assertDoesNotExist()
        rule.waitForDescription(str(R.string.add_points_for, "Nosotros"))
    }

    @Test
    fun deletingAHand_removesItAndItsPoints() {
        addHand("Ellos", "30")
        rule.waitForText(str(R.string.points_left, 70))

        openHand(1)
        rule.onNodeWithText(str(R.string.delete_hand)).performClick()

        rule.waitForNoText(str(R.string.hand_number, 1))
        assertEquals(2, rule.onAllNodesWithText(str(R.string.points_left, 100)).fetchSemanticsNodes().size)
        rule.onNodeWithText(str(R.string.no_hands)).assertExists()
    }

    @Test
    fun backArrow_callsOnBack() {
        rule.onNodeWithContentDescription(str(R.string.back)).performClick()
        assertTrue(backClicks == 1)
    }
}
