package com.error404.dominoapp.ui

import androidx.annotation.StringRes
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.printToString
import androidx.test.platform.app.InstrumentationRegistry

/** Reads a string from the app's strings.xml, so tests never hardcode UI text. */
fun str(@StringRes id: Int, vararg args: Any): String =
    InstrumentationRegistry.getInstrumentation().targetContext.getString(id, *args)

/** An editable field found by its label (e.g. "Equipo 1", "Puntos"). */
fun SemanticsNodeInteractionsProvider.field(label: String): SemanticsNodeInteraction =
    onNode(hasSetTextAction() and hasText(label))

// Room writes happen on a background thread that Compose tests don't wait for,
// so tests that depend on saved data wait for the screen to show it.

fun ComposeTestRule.waitForText(text: String, timeoutMs: Long = 5_000) =
    waitOrDescribeScreen("text \"$text\"", timeoutMs) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

fun ComposeTestRule.waitForNoText(text: String, timeoutMs: Long = 5_000) =
    waitOrDescribeScreen("no text \"$text\"", timeoutMs) { onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }

fun ComposeTestRule.waitForDescription(description: String, timeoutMs: Long = 5_000) =
    waitOrDescribeScreen("description \"$description\"", timeoutMs) {
        onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
    }

/** Like waitUntil, but on timeout the error lists everything that was on screen. */
private fun ComposeTestRule.waitOrDescribeScreen(what: String, timeoutMs: Long, condition: () -> Boolean) {
    try {
        waitUntil(timeoutMs, condition)
    } catch (e: ComposeTimeoutException) {
        throw AssertionError("Timed out waiting for $what. Screen was:\n" + onAllNodes(isRoot()).printToString(maxDepth = Int.MAX_VALUE), e)
    }
}
