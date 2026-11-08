package com.error404.dominoapp.ui

import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.core.app.ApplicationProvider
import com.error404.dominoapp.DominoApplication
import com.error404.dominoapp.MainActivity
import com.error404.dominoapp.R
import org.junit.Rule
import org.junit.rules.ExternalResource
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end journeys through the real app (MainActivity + navigation).
 * DominoTestRunner gives the app an in-memory database, so the device's saved matches are safe.
 */
@RunWith(AndroidJUnit4::class)
class AppNavigationTest {

    /**
     * The in-memory database lives as long as the test process, so matches from one test would
     * show up in the next one's history. Wipe it before the app opens (order = 0 runs first).
     */
    @get:Rule(order = 0)
    val emptyDatabase = object : ExternalResource() {
        override fun before() {
            ApplicationProvider.getApplicationContext<DominoApplication>().database.clearAllTables()
        }
    }

    @get:Rule(order = 1)
    val rule = createAndroidComposeRule<MainActivity>()

    private fun teamField(n: Int) = rule.field(str(R.string.team_name_hint, n))
    private val scoreboardTitle = str(R.string.scoreboard_title)

    private fun startMatch(target: String = "100") {
        teamField(1).performTextInput("Nosotros")
        teamField(2).performTextInput("Ellos")
        // The keyboard is still open and the pinned start bar rises with it, covering the chips:
        // scroll first, like a person would.
        rule.onNodeWithText(target).performScrollTo().performClick()
        rule.onNodeWithText(str(R.string.start_match)).performClick()
        // Don't wait for "Marcador"/"Nosotros": during the navigation animation both screens
        // are on screen at once. These only exist once the match has loaded from the database.
        rule.waitForText(str(R.string.target_info, target.toInt()))
        rule.waitForDescription(str(R.string.add_points_for, "Nosotros"))
    }

    private fun addHand(team: String, points: String) {
        rule.onNodeWithContentDescription(str(R.string.add_points_for, team)).performClick()
        val field = rule.field(str(R.string.editor_points))
        field.performTextInput(points)
        field.performImeAction()
        rule.waitForNoText(str(R.string.editor_new_title))
    }

    @Test
    fun fullMatch_fromSetupToWinner_andBackToAFreshForm() {
        startMatch()
        rule.onNodeWithText(str(R.string.target_info, 100)).assertExists()

        addHand("Nosotros", "60")
        rule.waitForText(str(R.string.points_left, 40))
        addHand("Nosotros", "45")
        rule.waitForText(str(R.string.winner_title, "Nosotros"))

        rule.onNodeWithText(str(R.string.new_match_title)).performClick()

        rule.waitForText(str(R.string.start_match))
        teamField(1).assertExists()
        rule.onNodeWithText("Nosotros").assertDoesNotExist() // the form starts empty again
    }

    @Test
    fun topBarBackArrow_returnsToAFreshForm() {
        startMatch()
        rule.onNodeWithContentDescription(str(R.string.back)).performClick()

        rule.waitForText(str(R.string.start_match))
        rule.onNodeWithText("Nosotros").assertDoesNotExist()
    }

    @Test
    fun systemBack_returnsFromScoreboardToTheForm() {
        startMatch()
        Espresso.pressBack()
        rule.waitForText(str(R.string.start_match))
    }

    @Test
    fun leavingAMatch_thenContinue_returnsToTheSameScoreboard() {
        startMatch()
        addHand("Nosotros", "25")
        rule.waitForText(str(R.string.points_left, 75))
        rule.onNodeWithContentDescription(str(R.string.back)).performClick()

        rule.waitForText(str(R.string.in_progress_title))
        rule.onNodeWithText(str(R.string.continue_match)).performScrollTo().performClick()

        rule.waitForText(str(R.string.points_left, 75))
        rule.onNodeWithText("+25").assertExists()
    }

    @Test
    fun finishedMatch_appearsInHistory_andOpensItsScoreboard() {
        startMatch()
        addHand("Ellos", "100")
        rule.waitForText(str(R.string.winner_title, "Ellos"))
        rule.onNodeWithText(str(R.string.new_match_title)).performClick()
        rule.waitForText(str(R.string.start_match))

        rule.onNodeWithContentDescription(str(R.string.open_history)).performClick()
        rule.waitForText(str(R.string.history_title))
        rule.waitForText(str(R.string.history_footer, 100, 1))

        rule.onNodeWithText(str(R.string.history_footer, 100, 1)).performClick()
        rule.waitForText(str(R.string.winner_title, "Ellos"))

        // Back twice: scoreboard -> history -> new match.
        Espresso.pressBack()
        rule.waitForText(str(R.string.history_footer, 100, 1))
        Espresso.pressBack()
        rule.waitForText(str(R.string.start_match))
    }

    @Test
    fun rotatingDuringSetup_keepsWhatWasTyped() {
        teamField(1).performTextInput("Nosotros")
        rule.onNodeWithText("250").performScrollTo().performClick()
        rule.field(str(R.string.target_score_custom)).assertTextContains("250") // the click landed

        rule.activityRule.scenario.recreate() // same as rotating the phone

        // recreate() builds a brand-new activity: wait until it has drawn before checking.
        rule.waitForText("Nosotros")
        teamField(1).assertTextContains("Nosotros")
        rule.field(str(R.string.target_score_custom)).assertTextContains("250")
    }

    @Test
    fun rotatingOnTheScoreboard_keepsTheMatch() {
        startMatch()
        addHand("Ellos", "35")
        rule.waitForText(str(R.string.points_left, 65))

        rule.activityRule.scenario.recreate()

        rule.waitForText(str(R.string.points_left, 65))
        rule.onNodeWithText(scoreboardTitle).assertExists()
        rule.onNodeWithText("+35").assertExists()
    }
}
