package com.deaddict.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

@Composable
fun AccountDeletionAction(
    visible: Boolean,
    inProgress: Boolean,
    onConfirm: () -> Unit,
) {
    if (!visible) return

    var confirmDelete by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd,
    ) {
        Button(
            onClick = { confirmDelete = true },
            enabled = !inProgress,
            modifier = Modifier
                .padding(end = 20.dp, bottom = 92.dp)
                .semantics { contentDescription = "Delete cloud account and all recovery data" },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            if (inProgress) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.padding(2.dp),
                )
            } else {
                Text("Delete account")
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = {
                if (!inProgress) confirmDelete = false
            },
            title = { Text("Delete account and all data?") },
            text = {
                Text(
                    "This permanently deletes your cloud account, cloud recovery data, and recovery data stored on this device. This cannot be undone.",
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !inProgress,
                    onClick = {
                        confirmDelete = false
                        onConfirm()
                    },
                ) {
                    Text("Delete permanently", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !inProgress,
                    onClick = { confirmDelete = false },
                ) {
                    Text("Cancel")
                }
            },
        )
    }
}
