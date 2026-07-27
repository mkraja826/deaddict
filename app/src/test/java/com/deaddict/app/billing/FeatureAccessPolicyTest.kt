package com.deaddict.app.billing

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureAccessPolicyTest {
    @Test
    fun `safety privacy rescue and deletion are never paywalled`() {
        val essential = listOf(
            AppFeature.RESCUE,
            AppFeature.SAFETY_RESOURCES,
            AppFeature.ACCOUNT_AND_DATA_DELETION,
            AppFeature.BIOMETRIC_PROTECTION,
            AppFeature.ESSENTIAL_PRIVACY,
        )

        essential.forEach {
            assertTrue(FeatureAccessPolicy.canAccess(it, EntitlementTier.FREE))
        }
    }

    @Test
    fun `plus features remain locked for free tier`() {
        assertFalse(
            FeatureAccessPolicy.canAccess(
                AppFeature.LONG_TERM_REPORTS,
                EntitlementTier.FREE,
            ),
        )
        assertTrue(
            FeatureAccessPolicy.canAccess(
                AppFeature.LONG_TERM_REPORTS,
                EntitlementTier.PLUS,
            ),
        )
    }
}
