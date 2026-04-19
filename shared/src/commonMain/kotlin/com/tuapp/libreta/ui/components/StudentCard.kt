package com.tuapp.libreta.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Student

// Paleta de avatares — Deep Blue / Slate profesional
private val avatarPalette = listOf(
    Color(0xFF1565C0) to Color(0xFFE3F2FD),
    Color(0xFF37474F) to Color(0xFFECEFF1),
    Color(0xFF00695C) to Color(0xFFE0F2F1),
    Color(0xFF4527A0) to Color(0xFFEDE7F6),
    Color(0xFF558B2F) to Color(0xFFF1F8E9),
    Color(0xFF6A1B9A) to Color(0xFFF3E5F5),
)

private fun avatarColors(name: String): Pair<Color, Color> =
    avatarPalette[name.hashCode().and(0x7FFFFFFF) % avatarPalette.size]

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentCard(
    student: Student,
    attendancePercent: Int = 100,
    onMarkPresent: (UuidString) -> Unit,
    onMarkAbsent: (UuidString) -> Unit,
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
        modifier         = modifier,
        backgroundContent = { SwipeBackground(dismissState) }
    ) {
        StudentCardContent(student, attendancePercent)
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
private fun StudentCardContent(student: Student, attendancePercent: Int) {
    val (iconColor, bgColor) = avatarColors(student.fullName)

    OutlinedCard(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Avatar circular con color único por alumno
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

            // Nombre + RUT
            Column(Modifier.weight(1f)) {
                Text(
                    text  = "${student.fullName}",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text  = student.courseId.value,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Badge de asistencia
            AttendanceBadge(percent = attendancePercent)
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
