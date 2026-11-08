package com.error404.dominoapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.error404.dominoapp.data.local.entity.HandEntity
import com.error404.dominoapp.data.local.entity.HandScoreEntity
import com.error404.dominoapp.data.local.entity.HandWithScores
import com.error404.dominoapp.data.local.entity.MatchEntity
import com.error404.dominoapp.data.local.entity.MatchWithParticipants
import com.error404.dominoapp.data.local.entity.ParticipantEntity
import com.error404.dominoapp.data.local.entity.ParticipantTotal
import kotlinx.coroutines.flow.Flow

// LEFT JOIN: a participant with no points yet still shows up with total 0.
private const val TOTALS_QUERY = """
    SELECT p.id AS participantId, p.name AS name, p.position AS position,
           COALESCE(SUM(s.points), 0) AS total
    FROM participants p
    LEFT JOIN hand_scores s ON s.participantId = p.id
    WHERE p.matchId = :matchId
    GROUP BY p.id
    ORDER BY p.position
"""

/**
 * Every database query.
 * - `suspend` functions run once (write, or read a single value).
 * - Functions returning `Flow` observe: they emit again whenever the tables change.
 */
@Dao
interface MatchDao {

    // ---------- Writes ----------

    @Insert
    suspend fun insertMatch(match: MatchEntity): Long

    @Insert
    suspend fun insertParticipants(participants: List<ParticipantEntity>): List<Long>

    @Insert
    suspend fun insertHand(hand: HandEntity): Long

    @Insert
    suspend fun insertScores(scores: List<HandScoreEntity>)

    @Update
    suspend fun updateMatch(match: MatchEntity)

    @Query("DELETE FROM hand_scores WHERE handId = :handId")
    suspend fun deleteScoresOfHand(handId: Long)

    @Query("DELETE FROM hands WHERE id = :handId")
    suspend fun deleteHand(handId: Long)

    @Query("DELETE FROM matches WHERE id = :matchId")
    suspend fun deleteMatch(matchId: Long)

    // ---------- One-shot reads ----------

    @Query("SELECT * FROM matches WHERE id = :matchId")
    suspend fun getMatch(matchId: Long): MatchEntity?

    @Query("SELECT * FROM hands WHERE id = :handId")
    suspend fun getHand(handId: Long): HandEntity?

    @Query("SELECT COALESCE(MAX(number), 0) + 1 FROM hands WHERE matchId = :matchId")
    suspend fun nextHandNumber(matchId: Long): Int

    @Query(TOTALS_QUERY)
    suspend fun getTotals(matchId: Long): List<ParticipantTotal>

    // ---------- Observed (Flow) ----------

    @Query("SELECT * FROM matches WHERE id = :matchId")
    fun observeMatch(matchId: Long): Flow<MatchEntity?>

    @Query(TOTALS_QUERY)
    fun observeTotals(matchId: Long): Flow<List<ParticipantTotal>>

    @Transaction
    @Query("SELECT * FROM hands WHERE matchId = :matchId ORDER BY number")
    fun observeHands(matchId: Long): Flow<List<HandWithScores>>

    @Transaction
    @Query("SELECT * FROM matches WHERE finishedAt IS NULL ORDER BY startedAt DESC LIMIT 1")
    fun observeMatchInProgress(): Flow<MatchWithParticipants?>

    @Transaction
    @Query("SELECT * FROM matches WHERE finishedAt IS NOT NULL ORDER BY finishedAt DESC")
    fun observeHistory(): Flow<List<MatchWithParticipants>>
}
