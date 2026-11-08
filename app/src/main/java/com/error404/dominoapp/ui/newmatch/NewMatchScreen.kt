package com.error404.dominoapp.ui.newmatch

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.error404.dominoapp.R
import com.error404.dominoapp.data.local.entity.MatchWithParticipants
import com.error404.dominoapp.data.local.entity.Mode
import com.error404.dominoapp.ui.theme.DominoAppTheme
import com.error404.dominoapp.ui.theme.dominoTopAppBarColors
import com.error404.dominoapp.ui.theme.participantColors

/** Stateful entry point: connects the ViewModel to the stateless [NewMatchContent]. */
@Composable
fun NewMatchScreen(
    onMatchStarted: (matchId: Long) -> Unit,
    onContinueMatch: (matchId: Long) -> Unit = {},
    onOpenHistory: () -> Unit = {},
    viewModel: NewMatchViewModel = viewModel(factory = NewMatchViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val matchInProgress by viewModel.matchInProgress.collectAsStateWithLifecycle()

    // Navigate exactly once when the match has been created.
    LaunchedEffect(state.startedMatchId) {
        state.startedMatchId?.let { matchId ->
            onMatchStarted(matchId)
            viewModel.onNavigated()
        }
    }

    NewMatchContent(
        state = state,
        matchInProgress = matchInProgress,
        onContinueMatch = onContinueMatch,
        onOpenHistory = onOpenHistory,
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
    matchInProgress: MatchWithParticipants?,
    onContinueMatch: (Long) -> Unit,
    onOpenHistory: () -> Unit,
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
                actions = {
                    IconButton(onClick = onOpenHistory) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.open_history))
                    }
                },
                colors = dominoTopAppBarColors(),
            )
        },
        // Pinned to the bottom: always visible, and rises above the keyboard.
        bottomBar = { StartButtonBar(state, onStart) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            matchInProgress?.let { ContinueMatchCard(it, onContinueMatch) }
            ModeSection(state.mode, onModeChange)
            NamesSection(state, onNameChange, onAddPlayer, onRemovePlayer)
            TargetScoreSection(state, onTargetScoreChange)
        }
    }
}

// ---------- Sections ----------

/** Offers to go back to the match left in progress (e.g. the app was closed mid-game). */
@Composable
private fun ContinueMatchCard(match: MatchWithParticipants, onContinue: (Long) -> Unit) {
    val names = match.participants
        .sortedBy { it.position }
        .joinToString(stringResource(R.string.names_separator)) { it.name }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.in_progress_title), style = MaterialTheme.typography.titleMedium)
            Text(names, style = MaterialTheme.typography.headlineSmall)
            Button(
                onClick = { onContinue(match.match.id) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    contentColor = MaterialTheme.colorScheme.onSecondary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.continue_match), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(R.string.in_progress_warning), style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** A titled card that groups one part of the form. */
@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            content()
        }
    }
}

@Composable
private fun ModeSection(selected: Mode, onModeChange: (Mode) -> Unit) {
    SectionCard(stringResource(R.string.mode_label)) {
        Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ModeCard(
                title = stringResource(R.string.mode_pairs),
                caption = stringResource(R.string.mode_pairs_caption),
                peopleIcons = 2,
                selected = selected == Mode.PAIRS,
                onClick = { onModeChange(Mode.PAIRS) },
                modifier = Modifier.weight(1f),
            )
            ModeCard(
                title = stringResource(R.string.mode_individual),
                caption = stringResource(R.string.mode_individual_caption),
                peopleIcons = 1,
                selected = selected == Mode.INDIVIDUAL,
                onClick = { onModeChange(Mode.INDIVIDUAL) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Big tappable card that behaves like a radio button. */
@Composable
private fun ModeCard(
    title: String,
    caption: String,
    peopleIcons: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.primaryContainer else colors.surfaceContainerHighest,
        contentColor = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        border = if (selected) BorderStroke(2.dp, colors.primary) else null,
        modifier = modifier.selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp),
        ) {
            // Overlapping person icons: two for pairs, one for individual.
            Row {
                repeat(peopleIcons) { i ->
                    Icon(
                        Icons.Filled.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(36.dp)
                            .offset(x = (-12 * i).dp),
                    )
                }
            }
            Spacer(Modifier.size(8.dp))
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(caption, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun NamesSection(
    state: NewMatchUiState,
    onNameChange: (Int, String) -> Unit,
    onAddPlayer: () -> Unit,
    onRemovePlayer: (Int) -> Unit,
) {
    val isPairs = state.mode == Mode.PAIRS
    SectionCard(stringResource(if (isPairs) R.string.teams_label else R.string.players_label)) {
        state.names.forEachIndexed { index, name ->
            val label = stringResource(if (isPairs) R.string.team_name_hint else R.string.player_name_hint, index + 1)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ParticipantAvatar(index, name)
                Spacer(Modifier.width(12.dp))
                TextField(
                    value = name,
                    onValueChange = { onNameChange(index, it) },
                    label = { Text(label) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    colors = filledFieldColors(),
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
            OutlinedButton(
                onClick = onAddPlayer,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.add_player))
            }
        }
    }
}

/** Colored circle with the name's initial (or the position number while empty). */
@Composable
private fun ParticipantAvatar(position: Int, name: String) {
    val colors = participantColors(position)
    Surface(shape = CircleShape, color = colors.container, contentColor = colors.content) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().firstOrNull()?.uppercase() ?: (position + 1).toString(),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
}

@Composable
private fun TargetScoreSection(state: NewMatchUiState, onTargetScoreChange: (String) -> Unit) {
    SectionCard(stringResource(R.string.target_score_label)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NewMatchUiState.TARGET_PRESETS.forEach { preset ->
                FilterChip(
                    selected = state.targetScore == preset,
                    onClick = { onTargetScoreChange(preset.toString()) },
                    label = {
                        Text(
                            preset.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp),
                )
            }
        }
        TextField(
            value = state.targetScoreText,
            onValueChange = onTargetScoreChange,
            label = { Text(stringResource(R.string.target_score_custom)) },
            isError = state.targetScore == null,
            supportingText = if (state.targetScore == null) {
                { Text(stringResource(R.string.target_score_error)) }
            } else {
                null
            },
            singleLine = true,
            shape = MaterialTheme.shapes.small,
            colors = filledFieldColors(),
            textStyle = MaterialTheme.typography.headlineSmall,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun StartButtonBar(state: NewMatchUiState, onStart: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 3.dp) {
        Button(
            onClick = onStart,
            enabled = state.canStart,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(16.dp)
                .heightIn(min = 56.dp),
        ) {
            if (state.isStarting) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.start_match), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

/** Filled, rounded field without the underline: softer than the default. */
@Composable
private fun filledFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    errorContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    // Hide the underline; the rounded container already marks the field.
    focusedIndicatorColor = Color.Transparent,
    unfocusedIndicatorColor = Color.Transparent,
    errorIndicatorColor = Color.Transparent,
)

@Preview(showBackground = true)
@Composable
private fun NewMatchContentPreview() {
    DominoAppTheme {
        NewMatchContent(
            state = NewMatchUiState(mode = Mode.INDIVIDUAL, names = listOf("Ana", "Luis", "")),
            matchInProgress = null, onContinueMatch = {}, onOpenHistory = {},
            onModeChange = {}, onNameChange = { _, _ -> }, onAddPlayer = {},
            onRemovePlayer = {}, onTargetScoreChange = {}, onStart = {},
        )
    }
}
