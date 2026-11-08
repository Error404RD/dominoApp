package com.error404.dominoapp.ui.newmatch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.error404.dominoapp.DominoApplication
import com.error404.dominoapp.data.local.entity.Mode
import com.error404.dominoapp.data.repository.MatchRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the "new match" form shows. The screen only draws this. */
data class NewMatchUiState(
    val mode: Mode = Mode.PAIRS,
    val names: List<String> = listOf("", ""),
    /** Kept as text so the field can be empty while the user types. */
    val targetScoreText: String = DEFAULT_TARGET_SCORE.toString(),
    val isStarting: Boolean = false,
    /** Set once the match exists; the screen navigates and then calls [NewMatchViewModel.onNavigated]. */
    val startedMatchId: Long? = null,
) {
    val targetScore: Int? get() = targetScoreText.toIntOrNull()?.takeIf { it > 0 }
    val canAddPlayer: Boolean get() = mode == Mode.INDIVIDUAL && names.size < MAX_PLAYERS
    val canRemovePlayer: Boolean get() = mode == Mode.INDIVIDUAL && names.size > MIN_PARTICIPANTS
    val canStart: Boolean get() = !isStarting && targetScore != null && names.none { it.isBlank() }

    companion object {
        const val DEFAULT_TARGET_SCORE = 200
        const val MIN_PARTICIPANTS = 2
        const val MAX_PLAYERS = 4
    }
}

class NewMatchViewModel(private val repository: MatchRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NewMatchUiState())
    val uiState: StateFlow<NewMatchUiState> = _uiState.asStateFlow()

    fun onModeChange(mode: Mode) = _uiState.update { state ->
        // Pairs always means exactly two teams; keep the first two names typed.
        val names = if (mode == Mode.PAIRS) state.names.take(NewMatchUiState.MIN_PARTICIPANTS) else state.names
        state.copy(mode = mode, names = names)
    }

    fun onNameChange(index: Int, name: String) = _uiState.update { state ->
        state.copy(names = state.names.toMutableList().also { it[index] = name })
    }

    fun onAddPlayer() = _uiState.update { state ->
        if (state.canAddPlayer) state.copy(names = state.names + "") else state
    }

    fun onRemovePlayer(index: Int) = _uiState.update { state ->
        if (state.canRemovePlayer) state.copy(names = state.names.filterIndexed { i, _ -> i != index }) else state
    }

    fun onTargetScoreChange(text: String) = _uiState.update { state ->
        // Digits only, max 4 (up to 9999 points).
        if (text.length <= 4 && text.all { it.isDigit() }) state.copy(targetScoreText = text) else state
    }

    fun startMatch() {
        val state = _uiState.value
        val target = state.targetScore ?: return
        if (!state.canStart) return

        _uiState.update { it.copy(isStarting = true) }
        viewModelScope.launch {
            val matchId = repository.startMatch(state.mode, state.names, target)
            _uiState.update { it.copy(isStarting = false, startedMatchId = matchId) }
        }
    }

    /** The screen already navigated; reset so going back here starts fresh. */
    fun onNavigated() = _uiState.update { NewMatchUiState() }

    companion object {
        /** Builds the ViewModel with the repository that lives in [DominoApplication]. */
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DominoApplication
                NewMatchViewModel(app.matchRepository)
            }
        }
    }
}
