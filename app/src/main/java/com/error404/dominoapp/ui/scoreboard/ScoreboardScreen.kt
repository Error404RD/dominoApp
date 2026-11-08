package com.error404.dominoapp.ui.scoreboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.error404.dominoapp.R
import com.error404.dominoapp.data.local.entity.HandEntity
import com.error404.dominoapp.data.local.entity.HandScoreEntity
import com.error404.dominoapp.data.local.entity.HandWithScores
import com.error404.dominoapp.data.local.entity.ParticipantTotal
import com.error404.dominoapp.ui.theme.DominoAppTheme
import com.error404.dominoapp.ui.theme.dominoTopAppBarColors
import com.error404.dominoapp.ui.theme.participantColors
import com.error404.dominoapp.ui.theme.winnerColors

/** Stateful entry point: connects the ViewModel to the stateless UI. */
@Composable
fun ScoreboardScreen(
    onBack: () -> Unit,
    onNewMatch: () -> Unit,
    viewModel: ScoreboardViewModel = viewModel(factory = ScoreboardViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val editor by viewModel.editor.collectAsStateWithLifecycle()

    ScoreboardContent(
        state = state,
        onBack = onBack,
        onNewMatch = onNewMatch,
        onAddPoints = viewModel::onAddPoints,
        onEditHand = viewModel::onEditHand,
    )

    editor?.let { current ->
        HandEditorSheet(
            editor = current,
            participants = state.totals,
            onParticipantChange = viewModel::onEditorParticipantChange,
            onPointsChange = viewModel::onEditorPointsChange,
            onSave = viewModel::onEditorSave,
            onDelete = viewModel::onDeleteHand,
            onDismiss = viewModel::onEditorDismiss,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoreboardContent(
    state: ScoreboardUiState,
    onBack: () -> Unit,
    onNewMatch: () -> Unit,
    onAddPoints: (participantId: Long) -> Unit,
    onEditHand: (HandWithScores) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scoreboard_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (state.targetScore > 0) {
                        Text(
                            stringResource(R.string.target_info, state.targetScore),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    }
                },
                colors = dominoTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.winner?.let { winner ->
                item(key = "winner") { WinnerBanner(winner, onNewMatch) }
            }
            if (state.isTiedAtTarget) {
                item(key = "tie") { TieNotice() }
            }
            item(key = "scores") {
                ScoreGrid(state, onAddPoints)
            }
            item(key = "handsTitle") {
                Text(
                    stringResource(R.string.hands_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (state.hands.isEmpty() && !state.isLoading) {
                item(key = "empty") {
                    Text(
                        stringResource(R.string.no_hands),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.hands, key = { it.hand.id }) { hand ->
                HandRow(hand, state.totals, onClick = { onEditHand(hand) })
            }
        }
    }
}

// ---------- Scores ----------

/** Two cards per row: 2 participants = one row, 3–4 = a 2×2 grid. */
@Composable
private fun ScoreGrid(state: ScoreboardUiState, onAddPoints: (Long) -> Unit) {
    val compact = state.totals.size > 2
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        state.totals.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { participant ->
                    ScoreCard(
                        participant = participant,
                        targetScore = state.targetScore,
                        isWinner = participant.participantId == state.winner?.participantId,
                        canAdd = state.canAddHands,
                        compact = compact,
                        onAdd = { onAddPoints(participant.participantId) },
                        modifier = Modifier.weight(1f),
                    )
                }
                // Odd count (3 players): keep the last card half-width.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ScoreCard(
    participant: ParticipantTotal,
    targetScore: Int,
    isWinner: Boolean,
    canAdd: Boolean,
    compact: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = participantColors(participant.position)
    val progress = if (targetScore > 0) (participant.total.toFloat() / targetScore).coerceIn(0f, 1f) else 0f
    val pointsLeft = (targetScore - participant.total).coerceAtLeast(0)

    Card(
        colors = CardDefaults.cardColors(containerColor = colors.container, contentColor = colors.content),
        border = if (isWinner) BorderStroke(4.dp, MaterialTheme.colorScheme.tertiary) else null,
        modifier = modifier,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isWinner) {
                    Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    participant.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                participant.total.toString(),
                style = if (compact) MaterialTheme.typography.displayMedium else MaterialTheme.typography.displayLarge,
            )
            LinearProgressIndicator(
                progress = { progress },
                color = colors.content,
                trackColor = colors.content.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            )
            Text(
                if (pointsLeft > 0) stringResource(R.string.points_left, pointsLeft) else stringResource(R.string.target_reached),
                style = MaterialTheme.typography.labelLarge,
            )
            if (canAdd) {
                Spacer(Modifier.size(12.dp))
                // Inverted colors so the button stands out on the colored card.
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.content, contentColor = colors.container),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.add_points),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
    }
}

@Composable
private fun WinnerBanner(winner: ParticipantTotal, onNewMatch: () -> Unit) {
    val sun = winnerColors()
    Card(
        colors = CardDefaults.cardColors(containerColor = sun.container, contentColor = sun.content),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
        ) {
            Icon(Icons.Filled.Star, contentDescription = null, modifier = Modifier.size(40.dp))
            Text(
                stringResource(R.string.winner_title, winner.name),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.winner_subtitle, winner.total),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.size(12.dp))
            Button(
                onClick = onNewMatch,
                colors = ButtonDefaults.buttonColors(containerColor = sun.content, contentColor = sun.container),
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.new_match_title), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun TieNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            stringResource(R.string.tie_message),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        )
    }
}

// ---------- Hands ----------

/** One played hand: its number and each participant's points, in their color. Tap to edit. */
@Composable
private fun HandRow(hand: HandWithScores, participants: List<ParticipantTotal>, onClick: () -> Unit) {
    val pointsById = hand.scores.associate { it.participantId to it.points }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .heightIn(min = 56.dp)
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                stringResource(R.string.hand_number, hand.hand.number),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.width(72.dp),
            )
            participants.forEach { participant ->
                val points = pointsById[participant.participantId] ?: 0
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    if (points > 0) {
                        val colors = participantColors(participant.position)
                        Surface(shape = CircleShape, color = colors.container, contentColor = colors.content) {
                            Text(
                                "+$points",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            )
                        }
                    } else {
                        Text(
                            "–",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Icon(
                Icons.Filled.Edit,
                contentDescription = stringResource(R.string.edit_hand, hand.hand.number),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ---------- Add / edit sheet ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HandEditorSheet(
    editor: HandEditorState,
    participants: List<ParticipantTotal>,
    onParticipantChange: (Long) -> Unit,
    onPointsChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    // Start with the whole number selected, so typing replaces it (handy when editing).
    var selection by remember { mutableStateOf(TextRange(0, editor.pointsText.length)) }
    // Open the keyboard right away: the user came here to type points.
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        ) {
            Text(
                if (editor.editingHandNumber == null) {
                    stringResource(R.string.editor_new_title)
                } else {
                    stringResource(R.string.editor_edit_title, editor.editingHandNumber)
                },
                style = MaterialTheme.typography.headlineSmall,
            )

            Text(stringResource(R.string.editor_who), style = MaterialTheme.typography.titleMedium)
            Row(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                participants.forEach { participant ->
                    ParticipantOption(
                        participant = participant,
                        selected = participant.participantId == editor.participantId,
                        onClick = { onParticipantChange(participant.participantId) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            TextField(
                // The text lives in the ViewModel; only the cursor/selection is local.
                // TextFieldValue clamps the selection if the ViewModel rejects a change.
                value = TextFieldValue(editor.pointsText, selection),
                onValueChange = { value ->
                    selection = value.selection
                    onPointsChange(value.text)
                },
                label = { Text(stringResource(R.string.editor_points)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.displaySmall,
                shape = MaterialTheme.shapes.small,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    // Hide the underline; the rounded container already marks the field.
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onSave() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (editor.editingHandId != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.delete_hand))
                    }
                }
                Spacer(Modifier.weight(1f))
                Button(
                    onClick = onSave,
                    enabled = editor.canSave,
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Text(stringResource(R.string.save), style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

/** Participant choice in the sheet, in that participant's color. Behaves like a radio button. */
@Composable
private fun ParticipantOption(
    participant: ParticipantTotal,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = participantColors(participant.position)
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = if (selected) colors.container else MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = if (selected) colors.content else MaterialTheme.colorScheme.onSurfaceVariant,
        border = if (selected) BorderStroke(2.dp, colors.content) else null,
        modifier = modifier
            .heightIn(min = 56.dp)
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(8.dp)) {
            Text(
                participant.name,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScoreboardPreview() {
    val totals = listOf(
        ParticipantTotal(participantId = 1, name = "Nosotros", position = 0, total = 145),
        ParticipantTotal(participantId = 2, name = "Ellos", position = 1, total = 90),
    )
    val hand = HandWithScores(
        hand = HandEntity(id = 1, matchId = 1, number = 1, playedAt = 0),
        scores = listOf(HandScoreEntity(handId = 1, participantId = 1, points = 45)),
    )
    DominoAppTheme {
        ScoreboardContent(
            state = ScoreboardUiState(isLoading = false, targetScore = 200, totals = totals, hands = listOf(hand)),
            onBack = {}, onNewMatch = {}, onAddPoints = {}, onEditHand = {},
        )
    }
}
