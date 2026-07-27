package com.deaddict.app.billing

enum class EntitlementTier { FREE, PLUS }

enum class AppFeature {
    RESCUE,
    SAFETY_RESOURCES,
    ACCOUNT_AND_DATA_DELETION,
    BIOMETRIC_PROTECTION,
    ESSENTIAL_PRIVACY,
    SINGLE_ACTIVE_PROGRAM,
    BASIC_TRACKING,
    DAILY_CHECK_IN,
    SEVEN_DAY_INSIGHTS,
    MULTIPLE_ACTIVE_PROGRAMS,
    ADVANCED_INSIGHTS,
    LONG_TERM_REPORTS,
    CLOUD_RESTORATION,
    ACCOUNTABILITY_PARTNER,
    CUSTOM_RESCUE_ROUTINES,
    ADVANCED_CONTROLS,
    FULL_EXPORT,
}

object FeatureAccessPolicy {
    private val plusFeatures = setOf(
        AppFeature.MULTIPLE_ACTIVE_PROGRAMS,
        AppFeature.ADVANCED_INSIGHTS,
        AppFeature.LONG_TERM_REPORTS,
        AppFeature.CLOUD_RESTORATION,
        AppFeature.ACCOUNTABILITY_PARTNER,
        AppFeature.CUSTOM_RESCUE_ROUTINES,
        AppFeature.ADVANCED_CONTROLS,
        AppFeature.FULL_EXPORT,
    )

    fun canAccess(feature: AppFeature, tier: EntitlementTier): Boolean =
        feature !in plusFeatures || tier == EntitlementTier.PLUS
}

enum class PurchaseVerificationStatus {
    NOT_REQUIRED,
    PENDING,
    BACKEND_UNAVAILABLE,
    VERIFIED,
    REJECTED,
}

data class SubscriptionOffer(
    val productId: String,
    val basePlanId: String,
    val offerToken: String,
    val formattedPrice: String,
    val billingPeriod: String,
)

data class BillingUiState(
    val connected: Boolean = false,
    val loading: Boolean = false,
    val offers: List<SubscriptionOffer> = emptyList(),
    val entitlement: EntitlementTier = EntitlementTier.FREE,
    val verification: PurchaseVerificationStatus = PurchaseVerificationStatus.NOT_REQUIRED,
    val message: String? = null,
)

interface PurchaseVerifier {
    suspend fun verify(productId: String, purchaseToken: String): PurchaseVerificationStatus
}

class DisconnectedPurchaseVerifier : PurchaseVerifier {
    override suspend fun verify(
        productId: String,
        purchaseToken: String,
    ): PurchaseVerificationStatus = PurchaseVerificationStatus.BACKEND_UNAVAILABLE
}
