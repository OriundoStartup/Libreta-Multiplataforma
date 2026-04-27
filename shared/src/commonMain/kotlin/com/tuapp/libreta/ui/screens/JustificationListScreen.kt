package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.JustificationStatus
import com.tuapp.libreta.presentation.JustificationListScreenModel
import com.tuapp.libreta.presentation.JustificationListUiState
import com.tuapp.libreta.ui.components.EmptyStateView

data class JustificationListScreen(val studentId: String) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: JustificationListScreenModel = koinScreenModel()
        val state by model.state.collectAsState()

        LaunchedEffect(studentId) { model.load(UuidString(studentId)) }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }},
                    title = { Text("Mis Justificaciones", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
                )
            }
        ) { padding ->
            when (val s = state) {
                JustificationListUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is JustificationListUiState.Success -> {
                    if (s.items.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Description,
                            title = "Sin justificaciones",
                            description = "Aún no has enviado trámites de inasistencia.",
                            modifier = Modifier.padding(padding)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(s.items) { item ->
                                JustificationItem(item)
                            }
                        }
                    }
                }
                is JustificationListUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun JustificationItem(item: Justification) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Motivo: ${item.reason}", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(4.dp))
                Text("Enviado: ${item.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            StatusBadge(status = item.status)
        }
    }
}

@Composable
private fun StatusBadge(status: JustificationStatus) {
    val (color, label) = when (status) {
        JustificationStatus.PENDING  -> Color(0xFFFFA000) to "Pendiente"
        JustificationStatus.APPROVED -> Color(0xFF2E7D32) to "Aprobada"
        JustificationStatus.REJECTED -> Color(0xFFC62828) to "Rechazada"
    }

    Surface(
        color = color.copy(alpha = 0.15f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = color
        )
    }
}
