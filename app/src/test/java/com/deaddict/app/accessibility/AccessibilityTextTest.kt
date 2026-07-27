package com.deaddict.app.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AccessibilityTextTest {
    @Test
    fun progressIsClampedAndRounded() {
        assertEquals("Goal, 0 percent", AccessibilityText.progress("Goal", -1.0))
        assertEquals("Goal, 56 percent", AccessibilityText.progress("Goal", 0.556))
        assertEquals("Goal, 100 percent", AccessibilityText.progress("Goal", 2.0))
    }

    @Test
    fun selectionAndStreakDescriptionsAreStable() {
        assertEquals("Alcohol, selected", AccessibilityText.selected("Alcohol", true))
        assertEquals("Alcohol, not selected", AccessibilityText.selected("Alcohol", false))
        assertEquals("1 day streak", AccessibilityText.streak(1))
        assertEquals("0 day streak", AccessibilityText.streak(-2))
    }

    @Test
    fun timerNeverAnnouncesNegativeTime() {
        assertEquals("Focus, 2 minutes 5 seconds remaining", AccessibilityText.timer("Focus", 125))
        assertEquals("Focus, 2 minutes remaining", AccessibilityText.timer("Focus", 120))
        assertEquals("Focus, 0 seconds remaining", AccessibilityText.timer("Focus", -1))
    }

    @Test
    fun privateValuesRemainHiddenByDefault() {
        assertEquals("Private note, hidden", AccessibilityText.privateValue("Private note", false, "secret"))
        assertEquals("Private note, secret", AccessibilityText.privateValue("Private note", true, "secret"))
    }

    @Test
    fun blankLabelsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            AccessibilityText.progress(" ", 0.5)
        }
    }
}
