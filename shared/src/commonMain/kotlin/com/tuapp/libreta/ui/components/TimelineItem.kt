package com.tuapp.libreta.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tuapp.libreta.presentation.TimelineEvent
import com.tuapp.libreta.presentation.TimelineEventType

@Composable
fun TimelineItem(
    event: TimelineEvent,
    isLast: Boolean = false,
    onJustify: (() -> Unit)? = null
) {
    val (icon, iconBg, iconTint) = when (event.type) {
        TimelineEventType.ATTENDANCE_PRESENT -> Triple(Icons.Default.CheckCircle, Color(0xFFE8F5E9), Color(0xFF2E7D32))
        TimelineEventType.ATTENDANCE_ABSENT  -> Triple(Icons.Default.Cancel,      Color(0xFFFFEBEE), Color(0xFFC62828))
        TimelineEventType.MESSAGE            -> Triple(Icons.Default.MailOutline,  Color(0xFFE3F2FD), Color(0xFF1565C0))
        TimelineEventType.JUSTIFICATION      -> Triple(Icons.Default.Description,  Color(0xFFFFF8E1), Color(0xFFF57F17))
    }

    Row(modifier = Modifier.fillMaxWidth()) {
        // Línea de tiempo vertical
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier         = Modifier.size(36.dp).clip(CircleShape).background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }
            if (!isLast) {
                Box(Modifier.width(2.dp).height(32.dp).background(MaterialTheme.colorScheme.outlineVariant))
            }
        }

        Spacer(Modifier.width(12.dp))

        // Contenido
        Column(
            modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text     = event.title,
                    style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text  = event.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text  = event.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (event.type == TimelineEventType.ATTENDANCE_ABSENT && onJustify != null) {
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick      = onJustify,
                    modifier     = Modifier.height(32.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape        = RoundedCornerShape(8.dp)
                ) {
                    Text("Justificar inasistencia", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
