package com.deaddict.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.deaddict.app.R

@Composable
fun PublicResourcesAction(
    visible: Boolean,
    onPrivacyPolicy: () -> Unit,
    onTermsOfService: () -> Unit,
    onSupport: () -> Unit,
    onAccountDeletionHelp: () -> Unit,
) {
    if (!visible) return

    var showResources by remember { mutableStateOf(false) }
    val buttonLabel = stringResource(R.string.public_resources_button)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomStart,
    ) {
        Button(
            onClick = { showResources = true },
            modifier = Modifier
                .padding(start = 20.dp, bottom = 92.dp)
                .testTag("public_resources")
                .semantics { contentDescription = buttonLabel },
        ) {
            Text(buttonLabel)
        }
    }

    if (showResources) {
        AlertDialog(
            onDismissRequest = { showResources = false },
            title = { Text(stringResource(R.string.public_resources_title)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.public_resources_description),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    ResourceLink(
                        label = stringResource(R.string.privacy_policy),
                        tag = "privacy_policy_link",
                        onClick = {
                            showResources = false
                            onPrivacyPolicy()
                        },
                    )
                    ResourceLink(
                        label = stringResource(R.string.terms_of_service),
                        tag = "terms_of_service_link",
                        onClick = {
                            showResources = false
                            onTermsOfService()
                        },
                    )
                    ResourceLink(
                        label = stringResource(R.string.support_center),
                        tag = "support_center_link",
                        onClick = {
                            showResources = false
                            onSupport()
                        },
                    )
                    ResourceLink(
                        label = stringResource(R.string.account_deletion_help),
                        tag = "account_deletion_help_link",
                        onClick = {
                            showResources = false
                            onAccountDeletionHelp()
                        },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showResources = false }) {
                    Text(stringResource(R.string.close))
                }
            },
        )
    }
}

@Composable
private fun ResourceLink(
    label: String,
    tag: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .testTag(tag),
    ) {
        Text(label, modifier = Modifier.fillMaxWidth())
    }
}
