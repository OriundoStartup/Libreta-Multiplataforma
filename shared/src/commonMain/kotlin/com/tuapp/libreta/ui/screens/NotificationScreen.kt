package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.presentation.NotificationItem
import com.tuapp.libreta.presentation.NotificationScreenModel
import com.tuapp.libreta.presentation.NotificationType
import com.tuapp.libreta.presentation.NotificationUiState
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.ui.components.EmptyStateView

data class NotificationScreen(
    val parentId: String,
    val studentIds: List<String>
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: NotificationScreenModel = koinScreenModel()
        val state by model.state.collectAsState()

        LaunchedEffect(Unit) {
            model.load(parentId, studentIds.map { UuidString(it) })
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    title = { Text("Notificaciones", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
                )
            }
        ) { padding ->
            when (val s = state) {
                NotificationUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is NotificationUiState.Success -> {
                    if (s.notifications.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Notifications,
                            title = "Sin actividad",
                            description = "Aquí aparecerán tus avisos, mensajes y cambios de asistencia.",
                            modifier = Modifier.padding(padding)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(padding)
                        ) {
                            items(s.notifications) { item ->
                                NotificationRow(item) {
                                    when (item.type) {
                                        NotificationType.MESSAGE -> {
                                            navigator.push(AppNavigation.messages()) // O una vista de chat específica si existe
                                        }
                                        NotificationType.ATTENDANCE -> {
                                            if (item.studentId != null) {
                                                navigator.push(AppNavigation.justificationForm(parentId = parentId, studentId = item.studentId))
                                            }
                                        }
                                        NotificationType.JUSTIFICATION -> {
                                            if (item.studentId != null) {
                                                navigator.push(AppNavigation.justificationList(item.studentId))
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }
                    }
                }
                is NotificationUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(item: NotificationItem, onClick: () -> Unit) {
    val (icon, color) = when (item.type) {
        NotificationType.MESSAGE       -> Icons.Default.Email to Color(0xFF1565C0)
        NotificationType.ATTENDANCE    -> Icons.Default.Close to Color(0xFFC62828)
        NotificationType.JUSTIFICATION -> Icons.Default.Done to Color(0xFF2E7D32)
        NotificationType.NOTICE        -> Icons.Default.Notifications to Color(0xFFF57F17)
        NotificationType.ANNOTATION    -> {
            if (item.content.contains("⚠️")) Icons.Default.Notifications to Color(0xFFD32F2F)
            else Icons.Default.Notifications to Color(0xFFFBC02D)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) MaterialTheme.colorScheme.surfaceContainerLow 
                             else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.weight(1f)
                    )
                    if (!item.isRead) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
                Text(item.content, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
                Text(
                    item.date,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}
