package com.error404.dominoapp.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.error404.dominoapp.R
import com.error404.dominoapp.data.local.entity.MatchEntity
import com.error404.dominoapp.data.local.entity.MatchParticipantTotal
import com.error404.dominoapp.data.local.entity.Mode
import com.error404.dominoapp.data.repository.MatchSummary
import com.error404.dominoapp.ui.theme.DominoAppTheme
import com.error404.dominoapp.ui.theme.dominoTopAppBarColors
import com.error404.dominoapp.ui.theme.participantColors
import java.text.DateFormat
import java.util.Date

/** Stateful entry point: connects the ViewModel to the stateless UI. */
@Composable
fun HistoryScreen(
    onBack: () -> Unit,
    onOpenMatch: (matchId: Long) -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pendingDelete by viewModel.pendingDelete.collectAsStateWithLifecycle()

    HistoryContent(
        state = state,
        onBack = onBack,
        onOpenMatch = onOpenMatch,
        onDeleteRequest = viewModel::onDeleteRequest,
    )

    pendingDelete?.let {
        DeleteMatchDialog(onConfirm = viewModel::onDeleteConfirm, onDismiss = viewModel::onDeleteDismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryContent(
    state: HistoryUiState,
    onBack: () -> Unit,
    onOpenMatch: (Long) -> Unit,
    onDeleteRequest: (MatchSummary) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = dominoTopAppBarColors(),
            )
        },
    ) { innerPadding ->
        if (state.matches.isEmpty() && !state.isLoading) {
            Text(
                stringResource(R.string.history_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(state.matches, key = { it.match.id }) { summary ->
                    MatchCard(
                        summary = summary,
                        onClick = { onOpenMatch(summary.match.id) },
                        onDelete = { onDeleteRequest(summary) },
                    )
                }
            }
        }
    }
}

/** One match: date, status, every participant's score (winner starred) and a delete button. */
@Composable
private fun MatchCard(summary: MatchSummary, onClick: () -> Unit, onDelete: () -> Unit) {
    val names = summary.totals.joinToString(stringResource(R.string.names_separator)) { it.name }
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    formatDate(summary.match.finishedAt ?: summary.match.startedAt),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                when {
                    summary.isInProgress -> StatusTag(
                        stringResource(R.string.status_in_progress),
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    summary.isAbandoned -> StatusTag(
                        stringResource(R.string.status_abandoned),
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                // IconButton is 48dp: meets the minimum touch size.
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.delete_match_cd, names),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 16.dp),
            ) {
                summary.totals.forEach { participant ->
                    ParticipantLine(participant, isWinner = participant.participantId == summary.match.winnerId)
                }
            }

            Text(
                stringResource(R.string.history_footer, summary.match.targetScore, summary.handCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun ParticipantLine(participant: MatchParticipantTotal, isWinner: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Small dot in the participant's color, same as on the other screens.
        Surface(shape = CircleShape, color = participantColors(participant.position).container) {
            Spacer(Modifier.size(14.dp))
        }
        Spacer(Modifier.width(12.dp))
        // Name + star take all the free space, so every score lines up on the right edge.
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Text(
                participant.name,
                style = if (isWinner) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isWinner) {
                Spacer(Modifier.width(4.dp))
                Icon(
                    Icons.Filled.Star,
                    contentDescription = stringResource(R.string.winner_cd),
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            participant.total.toString(),
            style = if (isWinner) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.titleLarge,
        )
    }
}

@Composable
private fun StatusTag(text: String, container: Color, content: Color) {
    Surface(shape = MaterialTheme.shapes.small, color = container, contentColor = content) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun DeleteMatchDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_match_title)) },
        text = { Text(stringResource(R.string.delete_match_text)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(R.string.delete)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Date and time in the phone's language and format (e.g. "8 nov 2026, 12:46"). */
@Composable
private fun formatDate(millis: Long): String {
    val format = remember { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT) }
    return format.format(Date(millis))
}

@Preview(showBackground = true)
@Composable
private fun HistoryPreview() {
    val won = MatchSummary(
        match = MatchEntity(id = 1, mode = Mode.PAIRS, targetScore = 200, startedAt = 0, finishedAt = 1, winnerId = 1),
        handCount = 7,
        totals = listOf(
            MatchParticipantTotal(1, 1, "Nosotros", 0, 205),
            MatchParticipantTotal(1, 2, "Ellos", 1, 150),
        ),
    )
    DominoAppTheme {
        HistoryContent(HistoryUiState(isLoading = false, matches = listOf(won)), {}, {}, {})
    }
}
