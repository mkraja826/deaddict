package com.deaddict.programs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgramRegistryTest {
    private val registry = DefaultProgramRegistry()

    @Test
    fun `registry exposes complete initial taxonomy`() {
        assertEquals(26, registry.all().size)
        assertEquals(8, registry.byCategory(ProgramCategory.SUBSTANCE).size)
        assertNotNull(registry.find(ProgramId.of("custom_habit")))
    }

    @Test
    fun `all tier C programs enforce every restriction`() {
        val tierC = registry.bySafetyTier(SafetyTier.MEDICALLY_HIGH_RISK)

        assertEquals(5, tierC.size)
        assertTrue(
            tierC.all {
                it.safety.professionalHelpPrompt &&
                    it.safety.emergencyEscalation &&
                    it.safety.prohibitedGuidance == ProhibitedGuidance.entries.toSet()
            },
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid program id is rejected`() {
        ProgramId.of("Not Valid")
    }
}

