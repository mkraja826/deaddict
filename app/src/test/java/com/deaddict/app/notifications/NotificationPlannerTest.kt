package com.deaddict.app.notifications

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPlannerTest {
    private val zone = ZoneId.of("UTC")

    @Test
    fun nextDaily_movesPastScheduleToTomorrow() {
        val now = epoch(2026, 7, 28, 10, 0)
        val reminders = NotificationPlanner.nextReminders(
            ReminderRules(dailyEnabled = true, dailyHour = 9),
            now,
            zone,
        )

        assertEquals(1, reminders.size)
        assertEquals(epoch(2026, 7, 29, 9, 0), reminders.single().atEpochMillis)
    }

    @Test
    fun quietHours_shiftReminderToQuietEnd() {
        val now = epoch(2026, 7, 28, 20, 0)
        val reminders = NotificationPlanner.nextReminders(
            ReminderRules(
                bedtimeEnabled = true,
                bedtimeHour = 23,
                quietStartHour = 22,
                quietEndHour = 7,
            ),
            now,
            zone,
        )

        assertEquals(epoch(2026, 7, 29, 7, 0), reminders.single().atEpochMillis)
    }

    @Test
    fun weeklyReminder_usesNextConfiguredDay() {
        val now = epoch(2026, 7, 28, 12, 0) // Tuesday
        val reminders = NotificationPlanner.nextReminders(
            ReminderRules(
                weeklyEnabled = true,
                weeklyDay = DayOfWeek.SUNDAY,
                weeklyHour = 18,
            ),
            now,
            zone,
        )

        assertEquals(epoch(2026, 8, 2, 18, 0), reminders.single().atEpochMillis)
    }

    @Test
    fun reminders_areSortedChronologically() {
        val now = epoch(2026, 7, 28, 8, 0)
        val reminders = NotificationPlanner.nextReminders(
            ReminderRules(
                dailyEnabled = true,
                dailyHour = 10,
                bedtimeEnabled = true,
                bedtimeHour = 21,
                weeklyEnabled = true,
                weeklyDay = DayOfWeek.TUESDAY,
                weeklyHour = 9,
                quietStartHour = 23,
                quietEndHour = 6,
            ),
            now,
            zone,
        )

        assertEquals(
            listOf(ReminderKind.WEEKLY_SUMMARY, ReminderKind.DAILY_CHECK_IN, ReminderKind.BEDTIME),
            reminders.map { it.kind },
        )
    }

    @Test
    fun quietTime_supportsOvernightAndSameHourDisabledWindows() {
        assertTrue(NotificationPlanner.isQuietTime(LocalTime.of(23, 0), 22, 7))
        assertTrue(NotificationPlanner.isQuietTime(LocalTime.of(6, 59), 22, 7))
        assertFalse(NotificationPlanner.isQuietTime(LocalTime.of(12, 0), 22, 7))
        assertFalse(NotificationPlanner.isQuietTime(LocalTime.of(22, 0), 22, 22))
    }

    @Test
    fun rules_rejectInvalidClockValues() {
        assertThrows(IllegalArgumentException::class.java) {
            ReminderRules(dailyHour = 24)
        }
    }

    private fun epoch(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()
}
