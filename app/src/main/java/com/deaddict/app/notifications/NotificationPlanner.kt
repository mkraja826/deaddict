package com.deaddict.app.notifications

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/** Pure scheduling rules used before handing work to WorkManager or AlarmManager. */
data class ReminderRules(
    val dailyEnabled: Boolean = false,
    val dailyHour: Int = 9,
    val dailyMinute: Int = 0,
    val bedtimeEnabled: Boolean = false,
    val bedtimeHour: Int = 21,
    val bedtimeMinute: Int = 0,
    val weeklyEnabled: Boolean = false,
    val weeklyDay: DayOfWeek = DayOfWeek.SUNDAY,
    val weeklyHour: Int = 18,
    val weeklyMinute: Int = 0,
    val quietStartHour: Int = 22,
    val quietEndHour: Int = 7,
) {
    init {
        require(dailyHour in 0..23 && dailyMinute in 0..59)
        require(bedtimeHour in 0..23 && bedtimeMinute in 0..59)
        require(weeklyHour in 0..23 && weeklyMinute in 0..59)
        require(quietStartHour in 0..23 && quietEndHour in 0..23)
    }
}

enum class ReminderKind { DAILY_CHECK_IN, BEDTIME, WEEKLY_SUMMARY }

data class PlannedReminder(val kind: ReminderKind, val atEpochMillis: Long)

object NotificationPlanner {
    fun nextReminders(
        rules: ReminderRules,
        nowEpochMillis: Long,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<PlannedReminder> {
        val now = Instant.ofEpochMilli(nowEpochMillis).atZone(zoneId)
        return buildList {
            if (rules.dailyEnabled) {
                add(PlannedReminder(ReminderKind.DAILY_CHECK_IN, nextDaily(now, rules.dailyHour, rules.dailyMinute, rules).toInstant().toEpochMilli()))
            }
            if (rules.bedtimeEnabled) {
                add(PlannedReminder(ReminderKind.BEDTIME, nextDaily(now, rules.bedtimeHour, rules.bedtimeMinute, rules).toInstant().toEpochMilli()))
            }
            if (rules.weeklyEnabled) {
                add(PlannedReminder(ReminderKind.WEEKLY_SUMMARY, nextWeekly(now, rules).toInstant().toEpochMilli()))
            }
        }.sortedBy { it.atEpochMillis }
    }

    fun isQuietTime(time: LocalTime, quietStartHour: Int, quietEndHour: Int): Boolean {
        require(quietStartHour in 0..23 && quietEndHour in 0..23)
        if (quietStartHour == quietEndHour) return false
        val hour = time.hour
        return if (quietStartHour < quietEndHour) {
            hour in quietStartHour until quietEndHour
        } else {
            hour >= quietStartHour || hour < quietEndHour
        }
    }

    private fun nextDaily(now: ZonedDateTime, hour: Int, minute: Int, rules: ReminderRules): ZonedDateTime {
        var candidate = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
        if (!candidate.isAfter(now)) candidate = candidate.plusDays(1)
        return outsideQuietHours(candidate, rules)
    }

    private fun nextWeekly(now: ZonedDateTime, rules: ReminderRules): ZonedDateTime {
        var date = now.toLocalDate()
        while (date.dayOfWeek != rules.weeklyDay) date = date.plusDays(1)
        var candidate = LocalDateTime.of(date, LocalTime.of(rules.weeklyHour, rules.weeklyMinute)).atZone(now.zone)
        if (!candidate.isAfter(now)) candidate = candidate.plusWeeks(1)
        return outsideQuietHours(candidate, rules)
    }

    private fun outsideQuietHours(candidate: ZonedDateTime, rules: ReminderRules): ZonedDateTime {
        if (!isQuietTime(candidate.toLocalTime(), rules.quietStartHour, rules.quietEndHour)) return candidate
        var shifted = candidate.withHour(rules.quietEndHour).withMinute(0).withSecond(0).withNano(0)
        if (!shifted.isAfter(candidate)) shifted = shifted.plusDays(1)
        return shifted
    }
}
