package com.deaddict.app.localization

import java.text.NumberFormat
import java.time.Duration
import java.util.Currency
import java.util.Locale
import kotlin.math.absoluteValue

/** Locale-aware formatting kept outside Compose so it is reusable and unit-testable. */
object LocalizedFormatter {
    fun integer(value: Long, locale: Locale): String =
        NumberFormat.getIntegerInstance(locale).format(value)

    fun decimal(value: Double, locale: Locale, maximumFractionDigits: Int = 1): String =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 0
            this.maximumFractionDigits = maximumFractionDigits.coerceAtLeast(0)
        }.format(value)

    fun moneyMinorUnits(
        minorUnits: Long,
        currencyCode: String,
        locale: Locale,
    ): String {
        val currency = Currency.getInstance(currencyCode)
        val divisor = powerOfTen(currency.defaultFractionDigits.coerceAtLeast(0))
        return NumberFormat.getCurrencyInstance(locale).apply {
            this.currency = currency
        }.format(minorUnits.toDouble() / divisor)
    }

    fun compactDuration(duration: Duration): String {
        require(!duration.isNegative) { "duration cannot be negative" }
        val totalMinutes = duration.toMinutes()
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            else -> "${minutes}m"
        }
    }

    private fun powerOfTen(exponent: Int): Long {
        var value = 1L
        repeat(exponent.absoluteValue) { value *= 10L }
        return value
    }
}
