package com.error404.dominoapp.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.error404.dominoapp.ui.newmatch.NewMatchScreen
import com.error404.dominoapp.ui.scoreboard.ScoreboardScreen

/** Route names. The scoreboard route carries the match id. */
private object Routes {
    const val NEW_MATCH = "new_match"
    const val SCOREBOARD = "scoreboard/{matchId}"
    fun scoreboard(matchId: Long) = "scoreboard/$matchId"
}

@Composable
fun DominoNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.NEW_MATCH) {
        composable(Routes.NEW_MATCH) {
            NewMatchScreen(onMatchStarted = { matchId -> navController.navigate(Routes.scoreboard(matchId)) })
        }
        composable(
            Routes.SCOREBOARD,
            // The ViewModel reads "matchId" from its SavedStateHandle.
            arguments = listOf(navArgument("matchId") { type = NavType.LongType }),
        ) {
            ScoreboardScreen(
                onBack = { navController.popBackStack() },
                onNewMatch = { navController.popBackStack(Routes.NEW_MATCH, inclusive = false) },
            )
        }
    }
}
