package com.error404.dominoapp.ui.scoreboard

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.error404.dominoapp.DominoApplication
import com.error404.dominoapp.data.local.entity.HandWithScores
import com.error404.dominoapp.data.local.entity.ParticipantTotal
import com.error404.dominoapp.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the scoreboard shows. Built from the database; never edited by hand. */
data class ScoreboardUiState(
    val isLoading: Boolean = true,
    val targetScore: Int = 0,
    /** One entry per participant, ordered by position. */
    val totals: List<ParticipantTotal> = emptyList(),
    /** Most recent hand first. */
    val hands: List<HandWithScores> = emptyList(),
    val winner: ParticipantTotal? = null,
    /** True once the match is over (won, or abandoned because a new one started). */
    val isFinished: Boolean = false,
) {
    val canAddHands: Boolean get() = !isLoading && !isFinished

    /** Closed without a winner because a new match was started: read-only. */
    val isAbandoned: Boolean get() = isFinished && winner == null

    /** Won matches can still be corrected (that may reopen them); abandoned ones cannot. */
    val canEditHands: Boolean get() = !isLoading && !isAbandoned

    /** Someone reached the target but the top is tied: nobody wins yet. */
    val isTiedAtTarget: Boolean get() = winner == null && !isFinished && totals.any { it.total >= targetScore }
}

/** The bottom sheet used to add a new hand or edit an existing one. */
data class HandEditorState(
    /** Null when adding a new hand. */
    val editingHandId: Long? = null,
    val editingHandNumber: Int? = null,
    val participantId: Long,
    val pointsText: String = "",
    val isSaving: Boolean = false,
) {
    val points: Int? get() = pointsText.toIntOrNull()?.takeIf { it > 0 }
    val canSave: Boolean get() = points != null && !isSaving
}

class ScoreboardViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: MatchRepository,
) : ViewModel() {

    /** Comes from the navigation route "scoreboard/{matchId}". */
    private val matchId: Long = checkNotNull(savedStateHandle["matchId"])

    val uiState: StateFlow<ScoreboardUiState> = combine(
        repository.observeMatch(matchId),
        repository.observeTotals(matchId),
        repository.observeHands(matchId),
    ) { match, totals, hands ->
        ScoreboardUiState(
            isLoading = false,
            targetScore = match?.targetScore ?: 0,
            totals = totals,
            hands = hands.sortedByDescending { it.hand.number },
            winner = totals.firstOrNull { it.participantId == match?.winnerId },
            isFinished = match?.finishedAt != null,
        )
    }.stateIn(
        scope = viewModelScope,
        // Keep listening 5s after the screen stops (e.g. during a rotation).
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ScoreboardUiState(),
    )

    private val _editor = MutableStateFlow<HandEditorState?>(null)
    /** Null = the sheet is closed. */
    val editor: StateFlow<HandEditorState?> = _editor.asStateFlow()

    fun onAddPoints(participantId: Long) {
        _editor.value = HandEditorState(participantId = participantId)
    }

    fun onEditHand(hand: HandWithScores) {
        // In the UI a hand has a single scorer: the one with the most points.
        val scorer = hand.scores.maxByOrNull { it.points } ?: return
        _editor.value = HandEditorState(
            editingHandId = hand.hand.id,
            editingHandNumber = hand.hand.number,
            participantId = scorer.participantId,
            pointsText = scorer.points.toString(),
        )
    }

    fun onEditorParticipantChange(participantId: Long) = _editor.update { it?.copy(participantId = participantId) }

    fun onEditorPointsChange(text: String) = _editor.update { editor ->
        // Digits only, max 3 (a domino hand never reaches 1000 points).
        if (editor != null && text.length <= 3 && text.all { it.isDigit() }) editor.copy(pointsText = text) else editor
    }

    fun onEditorDismiss() {
        _editor.value = null
    }

    fun onEditorSave() {
        val editor = _editor.value ?: return
        val points = editor.points ?: return
        if (!editor.canSave) return

        _editor.value = editor.copy(isSaving = true)
        viewModelScope.launch {
            val scores = mapOf(editor.participantId to points)
            try {
                if (editor.editingHandId == null) {
                    repository.saveHand(matchId, scores)
                } else {
                    repository.editHand(editor.editingHandId, scores)
                }
            } catch (e: IllegalArgumentException) {
                // The repository rejected the change (e.g. the match was abandoned meanwhile).
                // The screen keeps showing the database as it is, so just close the sheet.
            } finally {
                _editor.value = null
            }
        }
    }

    fun onDeleteHand() {
        val handId = _editor.value?.editingHandId ?: return
        _editor.value = null
        viewModelScope.launch { repository.deleteHand(handId) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DominoApplication
                ScoreboardViewModel(createSavedStateHandle(), app.matchRepository)
            }
        }
    }
}
