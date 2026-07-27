package com.deaddict.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayBillingManager @Inject constructor(
    @ApplicationContext context: Context,
) : PurchasesUpdatedListener {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val verifier: PurchaseVerifier = DisconnectedPurchaseVerifier()
    private val productDetailsByOfferToken = mutableMapOf<String, ProductDetails>()
    private val mutableState = MutableStateFlow(BillingUiState())
    val state: StateFlow<BillingUiState> = mutableState.asStateFlow()

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .enableAutoServiceReconnection()
        .build()

    fun connect() {
        if (client.isReady) {
            refresh()
            return
        }
        mutableState.value = mutableState.value.copy(loading = true)
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    mutableState.value = mutableState.value.copy(connected = true)
                    refresh()
                } else {
                    updateError(result.debugMessage)
                }
            }

            override fun onBillingServiceDisconnected() {
                mutableState.value = mutableState.value.copy(connected = false, loading = false)
            }
        })
    }

    fun disconnect() {
        if (client.isReady) client.endConnection()
        mutableState.value = mutableState.value.copy(connected = false)
    }

    fun refresh() {
        if (!client.isReady) return
        queryOffers()
        restorePurchases()
    }

    fun launchPurchase(activity: Activity, offerToken: String) {
        val details = productDetailsByOfferToken[offerToken] ?: run {
            mutableState.value = mutableState.value.copy(message = "That plan is unavailable.")
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offerToken)
                        .build(),
                ),
            )
            .build()
        val result = client.launchBillingFlow(activity, params)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            updateError(result.debugMessage)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: List<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> processPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> updateError(result.debugMessage)
        }
    }

    private fun queryOffers() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PLUS_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()
        client.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                updateError(result.debugMessage)
                return@queryProductDetailsAsync
            }
            productDetailsByOfferToken.clear()
            val offers = queryResult.productDetailsList.flatMap { details ->
                details.subscriptionOfferDetails.orEmpty().map { offer ->
                    productDetailsByOfferToken[offer.offerToken] = details
                    SubscriptionOffer(
                        productId = details.productId,
                        basePlanId = offer.basePlanId,
                        offerToken = offer.offerToken,
                        formattedPrice = offer.pricingPhases.pricingPhaseList.last().formattedPrice,
                        billingPeriod = offer.pricingPhases.pricingPhaseList.last().billingPeriod,
                    )
                }
            }
            mutableState.value = mutableState.value.copy(
                connected = true,
                loading = false,
                offers = offers,
                message = if (offers.isEmpty()) "Plus plans are not configured in Play yet." else null,
            )
        }
    }

    private fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                processPurchases(purchases)
            }
        }
    }

    private fun processPurchases(purchases: List<Purchase>) {
        val purchased = purchases.filter {
            it.purchaseState == Purchase.PurchaseState.PURCHASED &&
                PLUS_PRODUCT_ID in it.products
        }
        val pending = purchases.any {
            it.purchaseState == Purchase.PurchaseState.PENDING &&
                PLUS_PRODUCT_ID in it.products
        }
        if (purchased.isEmpty()) {
            mutableState.value = mutableState.value.copy(
                entitlement = EntitlementTier.FREE,
                verification = if (pending) {
                    PurchaseVerificationStatus.PENDING
                } else {
                    PurchaseVerificationStatus.NOT_REQUIRED
                },
                message = if (pending) "Purchase pending. Plus is enabled only after verification." else null,
            )
            return
        }
        mutableState.value = mutableState.value.copy(
            entitlement = EntitlementTier.FREE,
            verification = PurchaseVerificationStatus.PENDING,
            message = "Verifying purchase securely…",
        )
        scope.launch {
            val verification = verifier.verify(PLUS_PRODUCT_ID, purchased.first().purchaseToken)
            mutableState.value = mutableState.value.copy(
                entitlement = if (verification == PurchaseVerificationStatus.VERIFIED) {
                    EntitlementTier.PLUS
                } else {
                    EntitlementTier.FREE
                },
                verification = verification,
                message = when (verification) {
                    PurchaseVerificationStatus.BACKEND_UNAVAILABLE ->
                        "Purchase found. Secure verification resumes when cloud services reconnect."
                    PurchaseVerificationStatus.REJECTED -> "The purchase could not be verified."
                    PurchaseVerificationStatus.VERIFIED -> "DeAddict Plus restored."
                    else -> mutableState.value.message
                },
            )
        }
    }

    private fun updateError(message: String) {
        mutableState.value = mutableState.value.copy(
            connected = client.isReady,
            loading = false,
            message = message.ifBlank { "Google Play Billing is unavailable." },
        )
    }

    companion object {
        const val PLUS_PRODUCT_ID = "deaddict_plus"
    }
}
