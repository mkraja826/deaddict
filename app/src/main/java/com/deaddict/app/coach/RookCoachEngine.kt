package com.deaddict.app.coach

import com.deaddict.database.entity.TrackCheckInOutcome
import com.deaddict.programs.SafetyTier

enum class RookMoment {
    TODAY,
    TRACKS,
    RESCUE_READY,
    RESCUE_ACTIVE,
    RESCUE_COMPLETE,
    INSIGHTS,
}

enum class RookTrend {
    IMPROVING,
    STEADY,
    DECLINING,
    NOT_ENOUGH_DATA,
}

data class RookCoachRequest(
    val moment: RookMoment,
    val tone: RookTone,
    val trackId: String,
    val trackTitle: String,
    val safetyTier: SafetyTier,
    val outcome: TrackCheckInOutcome? = null,
    val initialUrge: Int? = null,
    val finalUrge: Int? = null,
    val adherencePercent: Int? = null,
    val streakDays: Int = 0,
    val trend: RookTrend = RookTrend.NOT_ENOUGH_DATA,
    val variationKey: Long = 0L,
)

data class RookCoachMessage(
    val title: String,
    val body: String,
    val toneUsed: RookTone,
    val safetyOverride: Boolean = false,
)

object RookCoachEngine {
    fun message(request: RookCoachRequest): RookCoachMessage {
        if (request.safetyTier == SafetyTier.MEDICALLY_HIGH_RISK) {
            return highRiskMessage(request)
        }

        val effectiveTone = when {
            request.safetyTier == SafetyTier.CLINICALLY_SENSITIVE &&
                request.tone == RookTone.BRUTAL_BANTER -> RookTone.DIRECT
            else -> request.tone
        }
        val base = when (request.moment) {
            RookMoment.TODAY -> todayMessage(request, effectiveTone)
            RookMoment.TRACKS -> trackMessage(request, effectiveTone)
            RookMoment.RESCUE_READY -> rescueReadyMessage(request, effectiveTone)
            RookMoment.RESCUE_ACTIVE -> rescueActiveMessage(request, effectiveTone)
            RookMoment.RESCUE_COMPLETE -> rescueCompleteMessage(request, effectiveTone)
            RookMoment.INSIGHTS -> insightsMessage(request, effectiveTone)
        }
        return if (request.safetyTier == SafetyTier.CLINICALLY_SENSITIVE) {
            base.copy(
                body = base.body +
                    " If this feels unsafe or unmanageable, involve a qualified professional.",
            )
        } else {
            base
        }
    }

    private fun highRiskMessage(request: RookCoachRequest): RookCoachMessage {
        val body = when (request.moment) {
            RookMoment.RESCUE_READY,
            RookMoment.RESCUE_ACTIVE,
            RookMoment.RESCUE_COMPLETE,
            -> "Your next move is safety, not bravado. Use qualified medical guidance for major changes, and contact local emergency services for severe withdrawal, overdose risk, or immediate danger."

            else -> "Keep ${request.trackTitle} honest and medically supported. DeAddict does not provide detox or taper instructions; use qualified guidance before major changes."
        }
        return RookCoachMessage(
            title = "Safety first",
            body = body,
            toneUsed = RookTone.DIRECT,
            safetyOverride = true,
        )
    }

    private fun todayMessage(
        request: RookCoachRequest,
        tone: RookTone,
    ): RookCoachMessage {
        val outcome = request.outcome
        if (outcome == null) {
            return create(
                tone = tone,
                title = "Rook check-in",
                request = request,
                direct = listOf(
                    "Give ${request.trackTitle} one honest answer. Facts first; explanations can wait.",
                    "Name what happened with ${request.trackTitle}. A clean record beats a polished excuse.",
                ),
                brutal = listOf(
                    "No courtroom speech. ${request.trackTitle} gets one honest answer and we move.",
                    "Your excuse department is closed. Log ${request.trackTitle} exactly as it happened.",
                ),
                quiet = listOf(
                    "Take a breath and record ${request.trackTitle} as it was. Honesty is enough for today.",
                    "A small truthful check-in for ${request.trackTitle} is useful progress.",
                ),
            )
        }

        val copy = when (outcome) {
            TrackCheckInOutcome.GOAL_MET -> Triple(
                listOf(
                    "Good. ${request.trackTitle} met the goal. Protect the next decision instead of celebrating early.",
                    "Goal met for ${request.trackTitle}. Keep the win boring and repeatable.",
                ),
                listOf(
                    "Nice work. Do not build a parade; build another day like this for ${request.trackTitle}.",
                    "${request.trackTitle} behaved today. Excellent. Now stop poking the victory like it owes you money.",
                ),
                listOf(
                    "${request.trackTitle} met the goal today. Notice what helped and carry it forward gently.",
                    "A steady day for ${request.trackTitle}. Let that be enough.",
                ),
            )

            TrackCheckInOutcome.GOAL_PARTLY_MET -> Triple(
                listOf(
                    "Partly met is data, not defeat. Keep what worked for ${request.trackTitle} and tighten one weak point.",
                    "${request.trackTitle} moved in the right direction. Choose one adjustment for tomorrow.",
                ),
                listOf(
                    "Halfway is not a throne, but it is not the floor either. Fix one leak in ${request.trackTitle} tomorrow.",
                    "${request.trackTitle} almost followed the plan. Good—now identify the bit that tried to wriggle out.",
                ),
                listOf(
                    "Some of the plan held for ${request.trackTitle}. Keep the useful part and make one gentle adjustment.",
                    "Partial progress still teaches you something. Notice what made the difference for ${request.trackTitle}.",
                ),
            )

            TrackCheckInOutcome.GOAL_NOT_MET -> Triple(
                listOf(
                    "The goal was not met. Do not hide the pattern—pick the next controllable move for ${request.trackTitle}.",
                    "Today missed the target for ${request.trackTitle}. Reset the plan, not your self-respect.",
                ),
                listOf(
                    "The plan lost this round. Fine. ${request.trackTitle} does not get tomorrow for free.",
                    "Target missed. No melodrama and no disappearing act—repair one decision for ${request.trackTitle}.",
                ),
                listOf(
                    "The goal was not met today. You can record it without judging yourself and choose one next step.",
                    "A difficult day for ${request.trackTitle} does not erase the work already done.",
                ),
            )

            TrackCheckInOutcome.SLIP -> Triple(
                listOf(
                    "Slip recorded for ${request.trackTitle}. Your other tracks are unchanged. Find the trigger and make the next decision smaller.",
                    "A slip is a signal, not a verdict. Protect the next hour for ${request.trackTitle}.",
                ),
                listOf(
                    "Slip logged. The habit got one point, not the whole season. Close the gap on ${request.trackTitle} now.",
                    "That was a slip, not a permission slip. ${request.trackTitle} still answers to your next decision.",
                ),
                listOf(
                    "The slip is recorded. Be kind, stay honest, and focus on the next safe choice for ${request.trackTitle}.",
                    "One difficult moment does not define this Recovery Track. Begin again with the next small action.",
                ),
            )

            TrackCheckInOutcome.AWARENESS_LOGGED -> Triple(
                listOf(
                    "Awareness logged for ${request.trackTitle}. Consistent observation is the job right now.",
                    "You recorded the pattern. Keep collecting honest evidence before forcing a conclusion.",
                ),
                listOf(
                    "Logged. The habit hates receipts, so keep collecting them for ${request.trackTitle}.",
                    "Another data point. Boring? Yes. Useful? Also yes.",
                ),
                listOf(
                    "You noticed and recorded ${request.trackTitle}. That quiet consistency matters.",
                    "Awareness is enough for this goal. Keep observing without pressure.",
                ),
            )
        }
        return create(
            tone = tone,
            title = outcomeTitle(outcome),
            request = request,
            direct = copy.first,
            brutal = copy.second,
            quiet = copy.third,
        )
    }

    private fun trackMessage(
        request: RookCoachRequest,
        tone: RookTone,
    ): RookCoachMessage = create(
        tone = tone,
        title = "${request.trackTitle} stays separate",
        request = request,
        direct = listOf(
            "This Recovery Track owns its goal, slips, Rescue history, and progress. Work the track in front of you.",
            "Keep ${request.trackTitle} specific. Progress here does not excuse or punish another track.",
        ),
        brutal = listOf(
            "One track, one scoreboard. ${request.trackTitle} cannot borrow excuses from the others.",
            "Keep the chaos in labeled boxes. Today we are dealing with ${request.trackTitle}.",
        ),
        quiet = listOf(
            "Focus only on ${request.trackTitle} for this moment. The other journeys remain separate.",
            "One Recovery Track at a time can make a complicated change feel manageable.",
        ),
    )

    private fun rescueReadyMessage(
        request: RookCoachRequest,
        tone: RookTone,
    ): RookCoachMessage = create(
        tone = tone,
        title = "Delay the decision",
        request = request,
        direct = listOf(
            "You do not need to win forever. Delay ${request.trackTitle} for two minutes and reduce the size of the decision.",
            "Do not negotiate at peak urge. Pause first; decide later.",
        ),
        brutal = listOf(
            "The urge has a sales pitch and terrible return policy. Give it two minutes of silence.",
            "Do not let a temporary urge act like the chief executive. Pause it.",
        ),
        quiet = listOf(
            "You only need to pause for two minutes. Let the urge rise and fall without deciding yet.",
            "Stay with the next breath. The decision can wait.",
        ),
    )

    private fun rescueActiveMessage(
        request: RookCoachRequest,
        tone: RookTone,
    ): RookCoachMessage = create(
        tone = tone,
        title = "Stay in the pause",
        request = request,
        direct = listOf(
            "Keep ${request.trackTitle} out of autopilot. Breathe, name the trigger, then choose one replacement action.",
            "The job is not to feel perfect. The job is to create enough space for a deliberate choice.",
        ),
        brutal = listOf(
            "Autopilot is trying to steal the steering wheel again. Hands back on it.",
            "The urge can complain. It still does not get the password.",
        ),
        quiet = listOf(
            "Stay with the pause. Notice the trigger and choose one small action when you are ready.",
            "You are creating space between the urge and the action. Keep breathing.",
        ),
    )

    private fun rescueCompleteMessage(
        request: RookCoachRequest,
        tone: RookTone,
    ): RookCoachMessage {
        val change = request.initialUrge?.let { initial ->
            request.finalUrge?.let { final -> initial - final }
        }
        val copy = when {
            change == null -> Triple(
                listOf("Rescue is complete. Record the result and keep the next decision small."),
                listOf("Rescue finished. No confetti cannon—just keep the next move clean."),
                listOf("The Rescue is complete. Take the next step gently."),
            )
            change > 0 -> Triple(
                listOf("The recorded urge dropped by $change point(s). Remember which action helped ${request.trackTitle}."),
                listOf("Urge down $change. Look at that—apparently pausing works better than arguing with yourself."),
                listOf("The urge softened by $change point(s). Notice what supported you."),
            )
            change == 0 -> Triple(
                listOf("The urge stayed level, but you interrupted autopilot. That still matters for ${request.trackTitle}."),
                listOf("Urge unchanged, decision delayed. Not glamorous, still useful."),
                listOf("The urge stayed the same, and you still made room before acting."),
            )
            else -> Triple(
                listOf("The urge rose by ${-change} point(s). Use support, change location, or repeat a safe replacement action."),
                listOf("Urge climbed. Fine—stop trying to out-stare it and change the environment."),
                listOf("The urge became stronger. Consider more support and a safer environment now."),
            )
        }
        return create(
            tone = tone,
            title = "Rescue recorded",
            request = request,
            direct = copy.first,
            brutal = copy.second,
            quiet = copy.third,
        )
    }

    private fun insightsMessage(
        request: RookCoachRequest,
        tone: RookTone,
    ): RookCoachMessage {
        val percent = request.adherencePercent
        val direct = when {
            percent == null -> "Keep checking in for ${request.trackTitle}. Rook needs evidence before making a claim."
            request.trend == RookTrend.IMPROVING -> "${request.trackTitle} is improving at $percent% in this window. Repeat the conditions behind the better days."
            request.trend == RookTrend.DECLINING -> "${request.trackTitle} is at $percent% and trending down. Pick one pattern to interrupt before it becomes the default."
            request.streakDays > 1 -> "${request.trackTitle} is at $percent% with a ${request.streakDays}-day run. Protect the routine that produced it."
            else -> "${request.trackTitle} is at $percent% in this window. Use the pattern, not the percentage, to choose the next move."
        }
        val brutal = when {
            percent == null -> "Rook cannot roast a blank spreadsheet. Log a few honest days for ${request.trackTitle}."
            request.trend == RookTrend.IMPROVING -> "$percent% and improving. Good. Do not immediately invent a reason to test your luck."
            request.trend == RookTrend.DECLINING -> "$percent% and sliding. The pattern is waving at you; stop pretending it is background scenery."
            request.streakDays > 1 -> "${request.streakDays} days in a row. Protect it—do not hand the habit a ceremonial comeback."
            else -> "$percent%. Useful number, terrible personality. Look at what actually changed for ${request.trackTitle}."
        }
        val quiet = when {
            percent == null -> "A few more honest check-ins will help ${request.trackTitle} show a clearer pattern."
            request.trend == RookTrend.IMPROVING -> "${request.trackTitle} is improving at $percent%. Notice the conditions that supported you."
            request.trend == RookTrend.DECLINING -> "The recent pattern is becoming harder. Choose one gentle, practical adjustment for ${request.trackTitle}."
            request.streakDays > 1 -> "A ${request.streakDays}-day run is forming. Keep the routine simple and steady."
            else -> "${request.trackTitle} is at $percent% in this window. Let the pattern guide one small next step."
        }
        return create(
            tone = tone,
            title = "Rook reads the pattern",
            request = request,
            direct = listOf(direct),
            brutal = listOf(brutal),
            quiet = listOf(quiet),
        )
    }

    private fun create(
        tone: RookTone,
        title: String,
        request: RookCoachRequest,
        direct: List<String>,
        brutal: List<String>,
        quiet: List<String>,
    ): RookCoachMessage {
        val candidates = when (tone) {
            RookTone.DIRECT -> direct
            RookTone.BRUTAL_BANTER -> brutal
            RookTone.QUIET -> quiet
        }
        return RookCoachMessage(
            title = title,
            body = candidates[stableIndex(request, candidates.size)],
            toneUsed = tone,
        )
    }

    private fun stableIndex(request: RookCoachRequest, size: Int): Int {
        if (size <= 1) return 0
        val hash = listOf(
            request.trackId,
            request.moment.name,
            request.outcome?.name.orEmpty(),
            request.variationKey.toString(),
        ).joinToString("|").hashCode().toLong()
        return ((hash and Long.MAX_VALUE) % size).toInt()
    }

    private fun outcomeTitle(outcome: TrackCheckInOutcome): String = when (outcome) {
        TrackCheckInOutcome.GOAL_MET -> "Goal met"
        TrackCheckInOutcome.GOAL_PARTLY_MET -> "Keep the useful part"
        TrackCheckInOutcome.GOAL_NOT_MET -> "Reset the next move"
        TrackCheckInOutcome.SLIP -> "Slip recorded, not identity"
        TrackCheckInOutcome.AWARENESS_LOGGED -> "Evidence collected"
    }
}
