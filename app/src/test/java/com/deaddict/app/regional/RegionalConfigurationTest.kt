package com.deaddict.app.regional

import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegionalConfigurationTest {
    @Test
    fun `India is the initial released market with configured languages`() {
        val config = RegionalConfigurationRegistry.forLocale(Locale("hi", "IN"))

        assertEquals(ReleaseMarket.INDIA, config.market)
        assertEquals("INR", config.currencyCode)
        assertTrue(config.released)
        assertTrue(config.supportedLanguageTags.containsAll(setOf("en", "hi", "te")))
    }

    @Test
    fun `unknown countries remain safely unreleased`() {
        val config = RegionalConfigurationRegistry.forCountry("ZZ")

        assertEquals(ReleaseMarket.UNRELEASED, config.market)
        assertFalse(config.released)
        assertEquals(null, config.currencyCode)
    }

    @Test
    fun `US stays reference-only until explicitly released`() {
        val config = RegionalConfigurationRegistry.forCountry("us")

        assertEquals(ReleaseMarket.UNITED_STATES, config.market)
        assertFalse(config.released)
        assertEquals("USD", config.currencyCode)
    }
}
