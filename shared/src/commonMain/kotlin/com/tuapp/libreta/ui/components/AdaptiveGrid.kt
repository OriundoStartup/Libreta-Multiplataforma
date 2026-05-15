package com.tuapp.libreta.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun <T> AdaptiveGrid(
    items: List<T>,
    columns: Int,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 16.dp,
    verticalSpacing: Dp = 16.dp,
    itemContent: @Composable (T) -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(verticalSpacing)
    ) {
        val rows = items.chunked(columns)
        for (rowItems in rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing)
            ) {
                for (item in rowItems) {
                    Box(modifier = Modifier.weight(1f)) {
                        itemContent(item)
                    }
                }
                // Rellenar espacios vacíos en la última fila si es necesario
                val emptySlots = columns - rowItems.size
                if (emptySlots > 0) {
                    for (i in 0 until emptySlots) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}
