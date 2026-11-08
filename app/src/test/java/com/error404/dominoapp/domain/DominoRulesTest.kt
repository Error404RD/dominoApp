package com.error404.dominoapp.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DominoRulesTest {

    private fun scores(vararg totals: Int) =
        totals.mapIndexed { i, total -> Score(participantId = i + 1L, total = total) }

    @Test
    fun nobodyReachedTarget_noWinner() {
        assertNull(DominoRules.decideWinner(scores(150, 199), targetScore = 200))
    }

    @Test
    fun exactlyTarget_wins() {
        assertEquals(2L, DominoRules.decideWinner(scores(150, 200), targetScore = 200))
    }

    @Test
    fun severalPassedTarget_highestWins() {
        assertEquals(1L, DominoRules.decideWinner(scores(230, 210, 50), targetScore = 200))
    }

    @Test
    fun tieAtTheTop_noWinnerYet() {
        assertNull(DominoRules.decideWinner(scores(210, 210, 90), targetScore = 200))
    }

    @Test
    fun tieBelowTheLeader_leaderStillWins() {
        assertEquals(3L, DominoRules.decideWinner(scores(205, 205, 240), targetScore = 200))
    }

    @Test
    fun noScores_noWinner() {
        assertNull(DominoRules.decideWinner(emptyList(), targetScore = 200))
    }
}
