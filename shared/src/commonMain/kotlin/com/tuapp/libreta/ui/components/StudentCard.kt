package com.tuapp.libreta.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.ui.theme.AvatarPalette

private fun avatarColors(name: String): Pair<Color, Color> =
    AvatarPalette[name.hashCode().and(0x7FFFFFFF) % AvatarPalette.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCard(
    student: Student,
    attendancePercent: Int = 100,
    onMarkPresent: (UuidString) -> Unit,
    onMarkAbsent: (UuidString) -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> { onMarkPresent(student.id); false }
                SwipeToDismissBoxValue.EndToStart -> { onMarkAbsent(student.id);  false }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state            = dismissState,
        modifier         = modifier.clip(RoundedCornerShape(16.dp)),
        backgroundContent = { SwipeBackground(dismissState) }
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 1.dp
        ) {
            val (iconColor, bgColor) = avatarColors(student.fullName)
            
            Row(
                modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Avatar circular
                Box(
                    modifier         = Modifier.size(48.dp).clip(CircleShape).background(bgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = student.fullName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = iconColor
                    )
                }

                // Nombre + Estado
                Column(Modifier.weight(1f)) {
                    Text(
                        text  = student.fullName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text  = "Registro regular", // En lugar de un ID críptico
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Badge de asistencia
                AttendanceBadge(percent = attendancePercent)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeBackground(state: SwipeToDismissBoxState) {
    val direction = state.dismissDirection
    val color by animateColorAsState(
        targetValue = when (direction) {
            SwipeToDismissBoxValue.StartToEnd -> Color(0xFF2E7D32)
            SwipeToDismissBoxValue.EndToStart -> Color(0xFFC62828)
            else                              -> Color.Transparent
        },
        label = "swipeBg"
    )
    val icon = if (direction == SwipeToDismissBoxValue.StartToEnd) Icons.Default.Check else Icons.Default.Close
    val alignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd

    Box(
        modifier          = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)).background(color).padding(horizontal = 20.dp),
        contentAlignment  = alignment
    ) {
        if (direction != SwipeToDismissBoxValue.Settled) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
    }
}

@Composable
fun AttendanceBadge(percent: Int) {
    val (bg, fg) = when {
        percent >= 85 -> Color(0xFFE8F5E9) to Color(0xFF2E7D32)
        percent >= 70 -> Color(0xFFFFF8E1) to Color(0xFFF57F17)
        else          -> Color(0xFFFFEBEE) to Color(0xFFC62828)
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text     = "$percent%",
            style    = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color    = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
