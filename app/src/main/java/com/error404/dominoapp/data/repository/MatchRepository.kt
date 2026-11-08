package com.error404.dominoapp.data.repository

import androidx.room.withTransaction
import com.error404.dominoapp.data.local.DominoDatabase
import com.error404.dominoapp.data.local.entity.HandEntity
import com.error404.dominoapp.data.local.entity.HandScoreEntity
import com.error404.dominoapp.data.local.entity.HandWithScores
import com.error404.dominoapp.data.local.entity.MatchEntity
import com.error404.dominoapp.data.local.entity.MatchWithParticipants
import com.error404.dominoapp.data.local.entity.Mode
import com.error404.dominoapp.data.local.entity.ParticipantEntity
import com.error404.dominoapp.data.local.entity.ParticipantTotal
import com.error404.dominoapp.domain.DominoRules
import com.error404.dominoapp.domain.Score
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * The single entry point the UI uses to read and change match data.
 *
 * Every write that can change totals (save / edit / delete a hand) ends by
 * calling [refreshWinner], so the winner is always consistent with the hands.
 *
 * @param now clock used for timestamps; tests pass a fake one.
 */
class MatchRepository(
    private val db: DominoDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.matchDao()

    // ---------- Observed data for the screens ----------

    fun observeMatch(matchId: Long): Flow<MatchEntity?> = dao.observeMatch(matchId)
    fun observeTotals(matchId: Long): Flow<List<ParticipantTotal>> = dao.observeTotals(matchId)
    fun observeHands(matchId: Long): Flow<List<HandWithScores>> = dao.observeHands(matchId)
    fun observeMatchInProgress(): Flow<MatchWithParticipants?> = dao.observeMatchInProgress()
    fun observeHistory(): Flow<List<MatchWithParticipants>> = dao.observeHistory()

    /** Every match with its totals, for the history screen. Updates whenever any match changes. */
    fun observeMatchSummaries(): Flow<List<MatchSummary>> =
        combine(dao.observeAllMatches(), dao.observeAllTotals()) { matches, totals ->
            val totalsByMatch = totals.groupBy { it.matchId }
            matches.map { MatchSummary(it.match, it.handCount, totalsByMatch[it.match.id].orEmpty()) }
        }

    // ---------- Commands ----------

    /**
     * Creates a new match and returns its id.
     * Any match still in progress is closed without a winner (abandoned), so
     * there is only ever one match in progress.
     */
    suspend fun startMatch(mode: Mode, names: List<String>, targetScore: Int): Long {
        val cleanNames = names.map { it.trim() }
        val allowedCount = if (mode == Mode.PAIRS) 2..2 else 2..4
        require(cleanNames.size in allowedCount) { "Invalid number of participants for $mode: ${cleanNames.size}" }
        require(cleanNames.none { it.isEmpty() }) { "Participant names cannot be empty" }
        require(targetScore > 0) { "Target score must be positive" }

        return db.withTransaction {
            val timestamp = now()
            dao.getMatchesInProgress().forEach { dao.updateMatch(it.copy(finishedAt = timestamp)) }

            val matchId = dao.insertMatch(MatchEntity(mode = mode, targetScore = targetScore, startedAt = timestamp))
            dao.insertParticipants(
                cleanNames.mapIndexed { i, name -> ParticipantEntity(matchId = matchId, name = name, position = i) },
            )
            matchId
        }
    }

    /**
     * Saves a new hand. [points] maps participantId -> points; participants left
     * out get 0. Returns the match's winnerId after saving (null if it goes on).
     */
    suspend fun saveHand(matchId: Long, points: Map<Long, Int>): Long? = db.withTransaction {
        val scores = buildScores(matchId, points)
        val handId = dao.insertHand(
            HandEntity(matchId = matchId, number = dao.nextHandNumber(matchId), playedAt = now()),
        )
        dao.insertScores(scores.map { it.copy(handId = handId) })
        refreshWinner(matchId)
    }

    /** Replaces the points of an existing hand. Returns the winnerId afterwards. */
    suspend fun editHand(handId: Long, points: Map<Long, Int>): Long? = db.withTransaction {
        val hand = requireNotNull(dao.getHand(handId)) { "Hand $handId does not exist" }
        val scores = buildScores(hand.matchId, points)
        dao.deleteScoresOfHand(handId)
        dao.insertScores(scores.map { it.copy(handId = handId) })
        refreshWinner(hand.matchId)
    }

    /** Deletes a hand (its scores go with it). Returns the winnerId afterwards. */
    suspend fun deleteHand(handId: Long): Long? = db.withTransaction {
        val hand = dao.getHand(handId) ?: return@withTransaction null
        dao.deleteHand(handId)
        refreshWinner(hand.matchId)
    }

    /** Deletes a match and everything in it (cascade). */
    suspend fun deleteMatch(matchId: Long) = dao.deleteMatch(matchId)

    // ---------- Internals ----------

    /** Validates [points] against the match's participants; missing ones get 0. */
    private suspend fun buildScores(matchId: Long, points: Map<Long, Int>): List<HandScoreEntity> {
        val match = requireNotNull(dao.getMatch(matchId)) { "Match $matchId does not exist" }
        require(!match.isAbandoned) { "Match $matchId was abandoned and can no longer change" }

        val participantIds = dao.getParticipants(matchId).map { it.id }
        val unknown = points.keys - participantIds.toSet()
        require(unknown.isEmpty()) { "Participants $unknown are not part of match $matchId" }
        require(points.values.all { it >= 0 }) { "Points cannot be negative" }

        // handId is filled in by the caller once the hand exists.
        return participantIds.map { id -> HandScoreEntity(handId = 0, participantId = id, points = points[id] ?: 0) }
    }

    /**
     * Recomputes the winner from the current totals and updates the match:
     * - someone wins -> mark winner and finish (keeps the original finish time if already finished)
     * - nobody wins  -> clear winner and reopen (a correction undid the win)
     * Abandoned matches are never touched. Returns the resulting winnerId.
     */
    private suspend fun refreshWinner(matchId: Long): Long? {
        val match = dao.getMatch(matchId) ?: return null
        if (match.isAbandoned) return null

        val scores = dao.getTotals(matchId).map { Score(it.participantId, it.total) }
        val winnerId = DominoRules.decideWinner(scores, match.targetScore)

        val updated = if (winnerId != null) {
            match.copy(winnerId = winnerId, finishedAt = match.finishedAt ?: now())
        } else {
            match.copy(winnerId = null, finishedAt = null)
        }
        if (updated != match) dao.updateMatch(updated)
        return winnerId
    }
}

/** Finished without a winner: closed because a new match was started. */
val MatchEntity.isAbandoned: Boolean get() = finishedAt != null && winnerId == null
