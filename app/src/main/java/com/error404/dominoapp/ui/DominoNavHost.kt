package com.error404.dominoapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.error404.dominoapp.R
import com.error404.dominoapp.ui.newmatch.NewMatchScreen
import com.error404.dominoapp.ui.theme.dominoTopAppBarColors

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
            arguments = listOf(navArgument("matchId") { type = NavType.LongType }),
        ) { entry ->
            val matchId = entry.arguments?.getLong("matchId") ?: return@composable
            ScoreboardPlaceholder(matchId)
        }
    }
}

/** Temporary screen until the real scoreboard exists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScoreboardPlaceholder(matchId: Long) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.scoreboard_title)) }, colors = dominoTopAppBarColors())
        },
    ) { innerPadding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.match_started_placeholder, matchId),
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}
