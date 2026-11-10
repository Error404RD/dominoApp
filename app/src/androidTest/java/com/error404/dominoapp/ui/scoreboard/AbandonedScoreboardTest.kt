package com.error404.dominoapp.ui.scoreboard

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.error404.dominoapp.R
import com.error404.dominoapp.data.local.DominoDatabase
import com.error404.dominoapp.data.local.entity.Mode
import com.error404.dominoapp.data.repository.MatchRepository
import com.error404.dominoapp.ui.plural
import com.error404.dominoapp.ui.str
import com.error404.dominoapp.ui.theme.DominoAppTheme
import com.error404.dominoapp.ui.waitForText
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A match closed because another one was started. The repository rejects any change to it,
 * so the scoreboard must be read-only (before, editing a hand here would crash the app).
 */
@RunWith(AndroidJUnit4::class)
class AbandonedScoreboardTest {

    @get:Rule
    val rule = createComposeRule()

    private lateinit var db: DominoDatabase

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), DominoDatabase::class.java)
            .build()
        val repository = MatchRepository(db)
        val abandonedId = runBlocking {
            val id = repository.startMatch(Mode.PAIRS, listOf("Nosotros", "Ellos"), targetScore = 100)
            val us = db.matchDao().getParticipants(id).first().id
            repository.saveHand(id, mapOf(us to 20))
            repository.startMatch(Mode.PAIRS, listOf("Otros", "Más"), targetScore = 100) // abandons the first
            id
        }
        val viewModel = ScoreboardViewModel(SavedStateHandle(mapOf("matchId" to abandonedId)), repository)
        rule.setContent {
            DominoAppTheme { ScoreboardScreen(onBack = {}, onNewMatch = {}, viewModel = viewModel) }
        }
        rule.waitForText(str(R.string.abandoned_notice))
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun showsTheNoticeAndNoAddButtons() {
        rule.onNodeWithContentDescription(str(R.string.add_points_for, "Nosotros")).assertDoesNotExist()
        rule.onNodeWithContentDescription(str(R.string.add_points_for, "Ellos")).assertDoesNotExist()
        rule.onNodeWithText(plural(R.plurals.points_left, 80)).assertExists()
    }

    @Test
    fun handsCannotBeEdited() {
        rule.onNodeWithText(str(R.string.hand_number, 1)).assertIsNotEnabled()
        rule.onNodeWithContentDescription(str(R.string.edit_hand, 1)).assertDoesNotExist()
    }
}
