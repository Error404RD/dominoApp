package com.error404.dominoapp.data.local.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** How the match is played. Room stores it as text ("PAIRS" / "INDIVIDUAL"). */
enum class Mode { PAIRS, INDIVIDUAL }

/** A match. If [finishedAt] is null, the match is still in progress. */
@Entity(tableName = "matches")
data class MatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: Mode,
    val targetScore: Int,
    val startedAt: Long,
    val finishedAt: Long? = null,
    // Intentionally not a foreign key: avoids a circular reference matches <-> participants.
    val winnerId: Long? = null,
)

/** A pair (team) or a single player inside a match. */
@Entity(
    tableName = "participants",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("matchId")],
)
data class ParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val name: String,
    /** Display order, so participants always appear in the same position. */
    val position: Int,
)

/** One round played inside a match. */
@Entity(
    tableName = "hands",
    foreignKeys = [
        ForeignKey(
            entity = MatchEntity::class,
            parentColumns = ["id"],
            childColumns = ["matchId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("matchId")],
)
data class HandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matchId: Long,
    val number: Int,
    val playedAt: Long,
)

/** The points one participant scored in one hand. */
@Entity(
    tableName = "hand_scores",
    foreignKeys = [
        ForeignKey(
            entity = HandEntity::class,
            parentColumns = ["id"],
            childColumns = ["handId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ParticipantEntity::class,
            parentColumns = ["id"],
            childColumns = ["participantId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("handId"), Index("participantId")],
)
data class HandScoreEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val handId: Long,
    val participantId: Long,
    val points: Int,
)

// ---------- Query results (not tables) ----------

/** A participant's running total, computed with SUM in the query. */
data class ParticipantTotal(
    val participantId: Long,
    val name: String,
    val position: Int,
    val total: Int,
)

/** A hand together with every participant's points. */
data class HandWithScores(
    @Embedded val hand: HandEntity,
    @Relation(parentColumn = "id", entityColumn = "handId")
    val scores: List<HandScoreEntity>,
)

/** A match with its participants (used by the history list). */
data class MatchWithParticipants(
    @Embedded val match: MatchEntity,
    @Relation(parentColumn = "id", entityColumn = "matchId")
    val participants: List<ParticipantEntity>,
)
