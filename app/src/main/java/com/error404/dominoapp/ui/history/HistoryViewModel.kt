package com.error404.dominoapp.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.error404.dominoapp.DominoApplication
import com.error404.dominoapp.data.repository.MatchRepository
import com.error404.dominoapp.data.repository.MatchSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HistoryUiState(
    val isLoading: Boolean = true,
    /** In-progress match first, then the most recently finished. */
    val matches: List<MatchSummary> = emptyList(),
)

class HistoryViewModel(private val repository: MatchRepository) : ViewModel() {

    val uiState: StateFlow<HistoryUiState> = repository.observeMatchSummaries()
        .map { HistoryUiState(isLoading = false, matches = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    private val _pendingDelete = MutableStateFlow<MatchSummary?>(null)
    /** The match waiting for the user to confirm its deletion (null = no dialog). */
    val pendingDelete: StateFlow<MatchSummary?> = _pendingDelete.asStateFlow()

    fun onDeleteRequest(match: MatchSummary) {
        _pendingDelete.value = match
    }

    fun onDeleteDismiss() {
        _pendingDelete.value = null
    }

    fun onDeleteConfirm() {
        val match = _pendingDelete.value ?: return
        _pendingDelete.value = null
        viewModelScope.launch { repository.deleteMatch(match.match.id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as DominoApplication
                HistoryViewModel(app.matchRepository)
            }
        }
    }
}
