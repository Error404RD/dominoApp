package com.error404.dominoapp.ui.newmatch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.error404.dominoapp.R
import com.error404.dominoapp.data.local.entity.Mode
import com.error404.dominoapp.ui.theme.DominoAppTheme
import com.error404.dominoapp.ui.theme.dominoTopAppBarColors

/** Stateful entry point: connects the ViewModel to the stateless [NewMatchContent]. */
@Composable
fun NewMatchScreen(
    onMatchStarted: (matchId: Long) -> Unit,
    viewModel: NewMatchViewModel = viewModel(factory = NewMatchViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate exactly once when the match has been created.
    LaunchedEffect(state.startedMatchId) {
        state.startedMatchId?.let { matchId ->
            onMatchStarted(matchId)
            viewModel.onNavigated()
        }
    }

    NewMatchContent(
        state = state,
        onModeChange = viewModel::onModeChange,
        onNameChange = viewModel::onNameChange,
        onAddPlayer = viewModel::onAddPlayer,
        onRemovePlayer = viewModel::onRemovePlayer,
        onTargetScoreChange = viewModel::onTargetScoreChange,
        onStart = viewModel::startMatch,
    )
}

/** Stateless UI: receives data and reports events. Easy to preview. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewMatchContent(
    state: NewMatchUiState,
    onModeChange: (Mode) -> Unit,
    onNameChange: (Int, String) -> Unit,
    onAddPlayer: () -> Unit,
    onRemovePlayer: (Int) -> Unit,
    onTargetScoreChange: (String) -> Unit,
    onStart: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.new_match_title)) },
                colors = dominoTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ----- Mode -----
            Text(stringResource(R.string.mode_label), style = MaterialTheme.typography.titleMedium)
            Column(Modifier.selectableGroup()) {
                ModeOption(stringResource(R.string.mode_pairs), state.mode == Mode.PAIRS) { onModeChange(Mode.PAIRS) }
                ModeOption(stringResource(R.string.mode_individual), state.mode == Mode.INDIVIDUAL) {
                    onModeChange(Mode.INDIVIDUAL)
                }
            }

            // ----- Names -----
            Text(
                stringResource(if (state.mode == Mode.PAIRS) R.string.teams_label else R.string.players_label),
                style = MaterialTheme.typography.titleMedium,
            )
            state.names.forEachIndexed { index, name ->
                val label = stringResource(
                    if (state.mode == Mode.PAIRS) R.string.team_name_hint else R.string.player_name_hint,
                    index + 1,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { onNameChange(index, it) },
                        label = { Text(label) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    if (state.canRemovePlayer) {
                        // IconButton is 48dp by default: meets the minimum touch size.
                        IconButton(onClick = { onRemovePlayer(index) }) {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.remove_player, label))
                        }
                    }
                }
            }
            if (state.canAddPlayer) {
                TextButton(onClick = onAddPlayer, modifier = Modifier.heightIn(min = 48.dp)) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(stringResource(R.string.add_player), modifier = Modifier.padding(start = 8.dp))
                }
            }

            // ----- Target score -----
            OutlinedTextField(
                value = state.targetScoreText,
                onValueChange = onTargetScoreChange,
                label = { Text(stringResource(R.string.target_score_label)) },
                isError = state.targetScore == null,
                supportingText = if (state.targetScore == null) {
                    { Text(stringResource(R.string.target_score_error)) }
                } else {
                    null
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.headlineSmall,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                modifier = Modifier.fillMaxWidth(),
            )

            // ----- Start -----
            Button(
                onClick = onStart,
                enabled = state.canStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            ) {
                if (state.isStarting) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.start_match), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** One radio row; the whole row is tappable and at least 48dp tall. */
@Composable
private fun ModeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null) // null: the Row handles the click
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun NewMatchContentPreview() {
    DominoAppTheme {
        NewMatchContent(
            state = NewMatchUiState(mode = Mode.INDIVIDUAL, names = listOf("Ana", "Luis", "")),
            onModeChange = {}, onNameChange = { _, _ -> }, onAddPlayer = {},
            onRemovePlayer = {}, onTargetScoreChange = {}, onStart = {},
        )
    }
}
