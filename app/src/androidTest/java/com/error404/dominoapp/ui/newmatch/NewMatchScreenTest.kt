package com.error404.dominoapp.ui.newmatch

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.error404.dominoapp.R
import com.error404.dominoapp.data.local.DominoDatabase
import com.error404.dominoapp.data.repository.MatchRepository
import com.error404.dominoapp.ui.field
import com.error404.dominoapp.ui.str
import com.error404.dominoapp.ui.theme.DominoAppTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The "Nueva partida" form, with its real ViewModel and an in-memory database. */
@RunWith(AndroidJUnit4::class)
class NewMatchScreenTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var db: DominoDatabase
    private lateinit var repository: MatchRepository
    private var startedMatchId: Long? = null

    private val startButton get() = rule.onNodeWithText(str(R.string.start_match))

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DominoDatabase::class.java)
            .build()
        repository = MatchRepository(db)
        // Created outside setContent so recompositions reuse the same ViewModel.
        val viewModel = NewMatchViewModel(repository)
        rule.setContent {
            DominoAppTheme {
                NewMatchScreen(onMatchStarted = { startedMatchId = it }, viewModel = viewModel)
            }
        }
    }

    @After
    fun tearDown() = db.close()

    private fun teamField(n: Int) = rule.field(str(R.string.team_name_hint, n))
    private fun playerField(n: Int) = rule.field(str(R.string.player_name_hint, n))
    private val targetField get() = rule.field(str(R.string.target_score_custom))

    @Test
    fun startsInPairsMode_withDefaultTarget() {
        rule.onNodeWithText(str(R.string.mode_pairs)).assertIsSelected()
        teamField(1).assertExists()
        teamField(2).assertExists()
        targetField.assertTextContains("200")
        rule.onNode(hasText("200") and isSelected()).assertExists() // the 200 chip
    }

    @Test
    fun startButton_isDisabledUntilEveryNameIsFilled() {
        startButton.assertIsNotEnabled()
        teamField(1).performTextInput("Nosotros")
        startButton.assertIsNotEnabled()
        teamField(2).performTextInput("Ellos")
        startButton.assertIsEnabled()
    }

    @Test
    fun blankName_onlySpaces_keepsStartDisabled() {
        teamField(1).performTextInput("Nosotros")
        teamField(2).performTextInput("   ")
        startButton.assertIsNotEnabled()
    }

    @Test
    fun individualMode_allowsBetweenTwoAndFourPlayers() {
        rule.onNodeWithText(str(R.string.mode_individual)).performClick()
        rule.onNodeWithText(str(R.string.players_label)).assertExists()

        val add = rule.onNodeWithText(str(R.string.add_player))
        add.performClick()
        add.performClick()
        playerField(4).assertExists()
        add.assertDoesNotExist() // 4 is the maximum

        rule.onNodeWithContentDescription(str(R.string.remove_player, str(R.string.player_name_hint, 4))).performClick()
        playerField(4).assertDoesNotExist()
        add.assertExists()
    }

    @Test
    fun backToPairs_keepsOnlyTheFirstTwoNames() {
        rule.onNodeWithText(str(R.string.mode_individual)).performClick()
        rule.onNodeWithText(str(R.string.add_player)).performClick()
        playerField(1).performTextInput("Ana")
        playerField(2).performTextInput("Luis")
        playerField(3).performTextInput("Eva")

        rule.onNodeWithText(str(R.string.mode_pairs)).performClick()

        teamField(1).assertTextContains("Ana")
        teamField(2).assertTextContains("Luis")
        rule.onNodeWithText("Eva").assertDoesNotExist()
    }

    @Test
    fun presetChip_setsTheTarget() {
        rule.onNodeWithText("150").performClick()
        targetField.assertTextContains("150")
        rule.onNode(hasText("150") and isSelected()).assertExists()
    }

    @Test
    fun emptyTarget_showsErrorAndDisablesStart() {
        teamField(1).performTextInput("Nosotros")
        teamField(2).performTextInput("Ellos")

        targetField.performTextClearance()

        rule.onNodeWithText(str(R.string.target_score_error)).assertExists()
        startButton.assertIsNotEnabled()
    }

    @Test
    fun targetField_ignoresLetters() {
        targetField.performTextClearance()
        // Typed one key at a time, like a person: "1" ok, "1a" rejected, "12" ok.
        listOf("1", "a", "2").forEach { targetField.performTextInput(it) }
        targetField.assertTextContains("12")
    }

    @Test
    fun start_createsTheMatchWithTheTypedNames() {
        teamField(1).performTextInput("Nosotros")
        teamField(2).performTextInput("Ellos")
        rule.onNodeWithText("100").performClick()

        startButton.performClick()

        rule.waitUntil(5_000) { startedMatchId != null }
        val matchId = startedMatchId!!
        runBlocking {
            assertEquals(listOf("Nosotros", "Ellos"), repository.observeTotals(matchId).first().map { it.name })
            assertEquals(100, repository.observeMatch(matchId).first()!!.targetScore)
        }
    }
}
