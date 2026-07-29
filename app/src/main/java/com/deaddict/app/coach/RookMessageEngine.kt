package com.deaddict.app.coach

enum class RookMoment {
    TODAY,
    TRACK,
    RESCUE,
    INSIGHTS,
}

data class RookContext(
    val moment: RookMoment,
    val programName: String,
    val activeTrackCount: Int,
    val requestedTone: RookTone,
    val medicallyHighRisk: Boolean,
    val slipRecorded: Boolean = false,
)

data class RookMessage(
    val text: String,
    val effectiveTone: RookTone,
)

object RookMessageEngine {
    fun message(context: RookContext): RookMessage? {
        if (context.requestedTone == RookTone.QUIET) return null

        val effectiveTone = if (context.medicallyHighRisk) {
            RookTone.DIRECT
        } else {
            context.requestedTone
        }

        val text = when {
            context.slipRecorded && effectiveTone == RookTone.BRUTAL_BANTER ->
                "One slip happened in ${context.programName}. Log the trigger, fix the next decision, and do not turn one event into a week-long collapse."

            context.slipRecorded ->
                "A slip was recorded for ${context.programName}. Your other recovery tracks are unchanged. Review the trigger and continue the plan."

            effectiveTone == RookTone.BRUTAL_BANTER -> brutalMessage(context)
            else -> directMessage(context)
        }

        return RookMessage(text = text, effectiveTone = effectiveTone)
    }

    private fun brutalMessage(context: RookContext): String = when (context.moment) {
        RookMoment.TODAY -> if (context.activeTrackCount > 1) {
            "${context.programName} is the focus right now. The other tracks still count, so do not use one win to hide another problem."
        } else {
            "${context.programName} is the target. You do not need another speech; make the next useful choice."
        }

        RookMoment.TRACK ->
            "Log ${context.programName} honestly. Hiding the numbers has never improved them."

        RookMoment.RESCUE ->
            "This urge is loud, not powerful. Stop negotiating with it and finish the Rescue steps for ${context.programName}."

        RookMoment.INSIGHTS -> if (context.activeTrackCount > 1) {
            "Check ${context.programName} without ignoring the other tracks. Replacement habits love hiding behind partial progress."
        } else {
            "The pattern is already in the data. Read it, own it, and change the next repeat."
        }
    }

    private fun directMessage(context: RookContext): String = when (context.moment) {
        RookMoment.TODAY -> if (context.activeTrackCount > 1) {
            "You are viewing ${context.programName}. Progress and lapses remain independent for every recovery track."
        } else {
            "Focus on the next helpful action for ${context.programName}."
        }

        RookMoment.TRACK ->
            "Record what happened for ${context.programName} as accurately as you can."

        RookMoment.RESCUE ->
            "Stay with the Rescue steps for ${context.programName}. You only need to make the next safe decision."

        RookMoment.INSIGHTS ->
            "Review the recent pattern for ${context.programName} and choose one adjustment."
    }
}
