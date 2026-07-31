package com.deaddict.app.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.deaddict.app.ui.theme.DeAddictTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PublicResourcesActionTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun hiddenActionDoesNotRender() {
        compose.setContent {
            DeAddictTheme {
                PublicResourcesAction(
                    visible = false,
                    onPrivacyPolicy = {},
                    onTermsOfService = {},
                    onSupport = {},
                    onAccountDeletionHelp = {},
                )
            }
        }

        compose.onNodeWithTag("public_resources").assertDoesNotExist()
    }

    @Test
    fun everyPublicResourceInvokesItsOwnAction() {
        var selectedResource: String? = null
        compose.setContent {
            DeAddictTheme {
                PublicResourcesAction(
                    visible = true,
                    onPrivacyPolicy = { selectedResource = "privacy" },
                    onTermsOfService = { selectedResource = "terms" },
                    onSupport = { selectedResource = "support" },
                    onAccountDeletionHelp = { selectedResource = "deletion" },
                )
            }
        }

        clickResource("privacy_policy_link")
        compose.runOnIdle { assertEquals("privacy", selectedResource) }

        clickResource("terms_of_service_link")
        compose.runOnIdle { assertEquals("terms", selectedResource) }

        clickResource("support_center_link")
        compose.runOnIdle { assertEquals("support", selectedResource) }

        clickResource("account_deletion_help_link")
        compose.runOnIdle { assertEquals("deletion", selectedResource) }
    }

    private fun clickResource(tag: String) {
        compose.onNodeWithTag("public_resources").performClick()
        compose.onNodeWithTag(tag).performClick()
    }
}
