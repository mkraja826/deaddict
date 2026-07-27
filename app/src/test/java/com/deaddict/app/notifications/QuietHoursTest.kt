package com.deaddict.app.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuietHoursTest {
    @Test
    fun `overnight quiet hours cross midnight`() {
        assertTrue(isQuietHour(23, 22, 7))
        assertTrue(isQuietHour(4, 22, 7))
        assertFalse(isQuietHour(12, 22, 7))
    }

    @Test
    fun `daytime quiet hours use same day range`() {
        assertTrue(isQuietHour(13, 9, 17))
        assertFalse(isQuietHour(18, 9, 17))
    }

    @Test
    fun `matching hours mean always quiet`() {
        assertTrue(isQuietHour(10, 8, 8))
    }
}

