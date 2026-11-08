package com.error404.dominoapp.data.repository

import com.error404.dominoapp.data.local.entity.MatchEntity
import com.error404.dominoapp.data.local.entity.MatchParticipantTotal

/** One row of the history: a match, its final (or current) totals and how many hands it had. */
data class MatchSummary(
    val match: MatchEntity,
    val handCount: Int,
    /** Ordered by position. */
    val totals: List<MatchParticipantTotal>,
) {
    val isInProgress: Boolean get() = match.finishedAt == null
    val isAbandoned: Boolean get() = match.isAbandoned
    val winner: MatchParticipantTotal? get() = totals.firstOrNull { it.participantId == match.winnerId }
}
