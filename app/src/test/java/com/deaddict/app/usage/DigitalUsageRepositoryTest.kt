package com.deaddict.app.usage

import org.junit.Assert.assertEquals
import org.junit.Test

class DigitalUsageRepositoryTest {
    @Test
    fun `session estimate splits openings after five minute gap`() {
        val openings = listOf(0L, 30_000L, 360_000L, 370_000L)

        assertEquals(2, openings.countStartsAfter(300_000L))
    }

    @Test
    fun `rapid reopening estimate counts only positive sub-minute gaps`() {
        val openings = listOf(1_000L, 40_000L, 100_000L, 100_000L, 159_999L)

        assertEquals(2, openings.countStartsWithin(60_000L))
    }

    @Test
    fun `empty event history has no estimated sessions`() {
        assertEquals(0, emptyList<Long>().countStartsAfter(300_000L))
    }
}

