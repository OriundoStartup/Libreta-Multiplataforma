package com.tuapp.libreta.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WindowSizeClass {
    COMPACT, // Phones
    MEDIUM,  // Tablets / Foldables
    EXPANDED // Desktop / Large Tablets
}

data class WindowSize(
    val widthSizeClass: WindowSizeClass,
    val heightSizeClass: WindowSizeClass,
    val width: Dp,
    val height: Dp
)

val LocalWindowSize = compositionLocalOf<WindowSize> {
    error("No WindowSize provided")
}

@Composable
fun ProvideWindowSize(width: Dp, height: Dp, content: @Composable () -> Unit) {
    val windowSize = WindowSize(
        widthSizeClass = when {
            width < 600.dp -> WindowSizeClass.COMPACT
            width < 840.dp -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        },
        heightSizeClass = when {
            height < 480.dp -> WindowSizeClass.COMPACT
            height < 900.dp -> WindowSizeClass.MEDIUM
            else -> WindowSizeClass.EXPANDED
        },
        width = width,
        height = height
    )
    
    CompositionLocalProvider(LocalWindowSize provides windowSize) {
        content()
    }
}
