package com.error404.dominoapp.domain

/** A participant's running total. Plain Kotlin: no Room or Android types here. */
data class Score(val participantId: Long, val total: Int)

object DominoRules {

    /**
     * Decides who wins the match, given everyone's current totals.
     *
     * Rules:
     * - Only participants whose total is >= [targetScore] can win.
     * - If several reached it, the one with the highest total wins.
     * - If the highest total is shared (a tie at the top), nobody wins yet: return null.
     * - If nobody reached [targetScore], return null.
     *
     * @return the winner's participantId, or null if the match goes on.
     */
    fun decideWinner(scores: List<Score>, targetScore: Int): Long? {
        // a) Only those who reached the target
        val reached = scores.filter { it.total >= targetScore }

        // b) Highest total among them (null if the list is empty)
        val best = reached.maxOfOrNull { it.total } ?: return null

        // c) Who has that highest total?
        val leaders = reached.filter { it.total == best }

        return if (leaders.size == 1) leaders.first().participantId else null
    }
}
