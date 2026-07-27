package com.deaddict.app.regional

import java.util.Locale

enum class ReleaseMarket {
    INDIA,
    UNITED_STATES,
    UNRELEASED,
}

data class RegionalConfiguration(
    val market: ReleaseMarket,
    val released: Boolean,
    val minimumAge: Int,
    val currencyCode: String?,
    val supportedLanguageTags: Set<String>,
    val emergencyGuidance: String,
)

object RegionalConfigurationRegistry {
    private val india = RegionalConfiguration(
        market = ReleaseMarket.INDIA,
        released = true,
        minimumAge = 18,
        currencyCode = "INR",
        supportedLanguageTags = setOf("en", "hi", "te"),
        emergencyGuidance = "Contact local emergency services or a qualified medical professional.",
    )
    private val unitedStates = RegionalConfiguration(
        market = ReleaseMarket.UNITED_STATES,
        released = false,
        minimumAge = 18,
        currencyCode = "USD",
        supportedLanguageTags = setOf("en"),
        emergencyGuidance = "Call 911 for an immediate emergency or contact a qualified medical professional.",
    )
    private val unreleased = RegionalConfiguration(
        market = ReleaseMarket.UNRELEASED,
        released = false,
        minimumAge = 18,
        currencyCode = null,
        supportedLanguageTags = setOf("en"),
        emergencyGuidance = "Contact local emergency services or a qualified medical professional.",
    )

    fun forCountry(countryCode: String): RegionalConfiguration = when (
        countryCode.uppercase(Locale.ROOT)
    ) {
        "IN" -> india
        "US" -> unitedStates
        else -> unreleased
    }

    fun forLocale(locale: Locale): RegionalConfiguration = forCountry(locale.country)
}
