package com.deaddict.app.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** Keeps the explicit root source readable while resolving Material3's clickable Card parameter order. */
internal typealias Column = ColumnScope

@Composable
internal fun Card(
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        modifier = modifier,
        content = content,
    )
}
