package com.deaddict.app.rescue

import com.deaddict.programs.ProgramDefinition

enum class RescueStep {
    READY,
    PAUSE,
    MOTIVATION,
    INITIAL_URGE,
    TRIGGER,
    REPLACEMENT,
    RECHECK,
    COMPLETE,
}

data class RescueFlowState(
    val step: RescueStep = RescueStep.READY,
    val recoveryTrackId: String? = null,
    val program: ProgramDefinition? = null,
    val startedAtEpochMillis: Long? = null,
    val secondsRemaining: Int = PAUSE_SECONDS,
    val motivation: String = "I want more choice in what happens next.",
    val initialUrge: Int = 3,
    val finalUrge: Int = 3,
    val triggerKey: String? = null,
    val replacementActions: List<String> = emptyList(),
    val selectedAction: String? = null,
) {
    val isOwned: Boolean
        get() = recoveryTrackId != null && program != null && startedAtEpochMillis != null

    companion object {
        const val PAUSE_SECONDS = 60
    }
}

class RescueFlow {
    fun begin(
        recoveryTrackId: String,
        program: ProgramDefinition,
        startedAtEpochMillis: Long,
    ): RescueFlowState {
        require(recoveryTrackId.isNotBlank()) { "Recovery Track ID is required" }
        require(startedAtEpochMillis > 0) { "Rescue start time must be positive" }
        return RescueFlowState(
            step = RescueStep.PAUSE,
            recoveryTrackId = recoveryTrackId,
            program = program,
            startedAtEpochMillis = startedAtEpochMillis,
        )
    }

    fun tick(state: RescueFlowState): RescueFlowState {
        require(state.step == RescueStep.PAUSE)
        require(state.isOwned) { "Rescue flow must remain attached to a Recovery Track" }
        return state.copy(secondsRemaining = (state.secondsRemaining - 1).coerceAtLeast(0))
    }

    fun continueAfterPause(state: RescueFlowState): RescueFlowState {
        require(state.step == RescueStep.PAUSE && state.secondsRemaining == 0)
        require(state.isOwned)
        return state.copy(step = RescueStep.MOTIVATION)
    }

    fun acknowledgeMotivation(state: RescueFlowState): RescueFlowState {
        require(state.step == RescueStep.MOTIVATION)
        return state.copy(step = RescueStep.INITIAL_URGE)
    }

    fun setInitialUrge(state: RescueFlowState, intensity: Int): RescueFlowState {
        require(state.step == RescueStep.INITIAL_URGE)
        require(intensity in 1..5)
        return state.copy(initialUrge = intensity, finalUrge = intensity)
    }

    fun continueToTrigger(state: RescueFlowState): RescueFlowState {
        require(state.step == RescueStep.INITIAL_URGE)
        return state.copy(step = RescueStep.TRIGGER)
    }

    fun chooseTrigger(state: RescueFlowState, triggerKey: String): RescueFlowState {
        require(state.step == RescueStep.TRIGGER)
        require(triggerKey in VALID_TRIGGERS)
        return state.copy(
            step = RescueStep.REPLACEMENT,
            triggerKey = triggerKey,
            replacementActions = actionsFor(triggerKey),
        )
    }

    fun chooseReplacement(state: RescueFlowState, action: String): RescueFlowState {
        require(state.step == RescueStep.REPLACEMENT)
        require(action in state.replacementActions)
        return state.copy(step = RescueStep.RECHECK, selectedAction = action)
    }

    fun setFinalUrge(state: RescueFlowState, intensity: Int): RescueFlowState {
        require(state.step == RescueStep.RECHECK)
        require(intensity in 1..5)
        return state.copy(finalUrge = intensity)
    }

    fun complete(state: RescueFlowState): RescueFlowState {
        require(state.step == RescueStep.RECHECK)
        require(state.isOwned) { "Completed Rescue sessions require Recovery Track ownership" }
        return state.copy(step = RescueStep.COMPLETE)
    }

    fun reset(): RescueFlowState = RescueFlowState()

    private fun actionsFor(trigger: String): List<String> = when (trigger) {
        "stress" -> listOf("Take ten slow breaths", "Walk for two minutes", "Contact someone you trust")
        "boredom" -> listOf("Change your location", "Start a tiny task", "Drink a glass of water")
        "social" -> listOf("Step away briefly", "Message a supportive person", "Name your boundary")
        "routine" -> listOf("Interrupt the usual sequence", "Delay for ten minutes", "Choose a replacement ritual")
        else -> listOf("Move away from access", "Breathe and delay", "Contact someone you trust")
    }

    companion object {
        val VALID_TRIGGERS = setOf("stress", "boredom", "social", "routine", "access")
    }
}
