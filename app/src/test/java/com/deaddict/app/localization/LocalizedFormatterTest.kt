package com.deaddict.app.localization

import java.time.Duration
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalizedFormatterTest {
    @Test
    fun formatsNumbersUsingRequestedLocale() {
        assertEquals("12,345", LocalizedFormatter.integer(12_345, Locale.US))
        assertEquals("12.345", LocalizedFormatter.integer(12_345, Locale.GERMANY))
        assertEquals("1.5", LocalizedFormatter.decimal(1.54, Locale.US))
    }

    @Test
    fun formatsMinorCurrencyUnitsWithoutFloatingPointInput() {
        assertEquals("\$12.34", LocalizedFormatter.moneyMinorUnits(1_234, "USD", Locale.US))

        val yen = LocalizedFormatter.moneyMinorUnits(1_234, "JPY", Locale.JAPAN)
        assertTrue(yen.contains("1,234"))
        assertTrue(yen.contains('¥') || yen.contains('￥'))
    }

    @Test
    fun formatsCompactDurations() {
        assertEquals("45m", LocalizedFormatter.compactDuration(Duration.ofMinutes(45)))
        assertEquals("2h", LocalizedFormatter.compactDuration(Duration.ofHours(2)))
        assertEquals("2h 5m", LocalizedFormatter.compactDuration(Duration.ofMinutes(125)))
    }

    @Test
    fun rejectsNegativeDurations() {
        assertThrows(IllegalArgumentException::class.java) {
            LocalizedFormatter.compactDuration(Duration.ofMinutes(-1))
        }
    }
}
