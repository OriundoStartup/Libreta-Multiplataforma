package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.presentation.GlobalJustificationScreenModel
import com.tuapp.libreta.presentation.GlobalJustificationUiState
import com.tuapp.libreta.ui.components.EmptyStateView

object GlobalJustificationReviewScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: GlobalJustificationScreenModel = koinScreenModel()
        val state by model.state.collectAsState()

        LaunchedEffect(Unit) { model.load() }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    title = { Text("Trámites Pendientes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) }
                )
            }
        ) { padding ->
            when (val s = state) {
                GlobalJustificationUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is GlobalJustificationUiState.Success -> {
                    if (s.pending.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.AssignmentTurnedIn,
                            title = "Todo al día",
                            description = "No hay justificaciones pendientes por revisar.",
                            modifier = Modifier.padding(padding)
                        )
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(padding)
                        ) {
                            items(s.pending) { item ->
                                PendingJustificationItem(
                                    item = item,
                                    onApprove = { model.review(item, true) },
                                    onReject = { model.review(item, false) }
                                )
                            }
                        }
                    }
                }
                is GlobalJustificationUiState.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun PendingJustificationItem(
    item: Justification,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Description, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(20.dp))
                    }
                }
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text("Inasistencia del ${item.date}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.reason, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                }
            }

            if (item.documentUrl != null) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(top = 12.dp).fillMaxWidth()
                ) {
                    Text(
                        "📎 Certificado adjunto disponible",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF2E7D32)
                    )
                }
            }

            Row(
                modifier = Modifier.padding(top = 16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f).height(44.dp).background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Close, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        Text("Rechazar", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(start = 4.dp))
                    }
                }

                IconButton(
                    onClick = onApprove,
                    modifier = Modifier.weight(1f).height(44.dp).background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Check, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(18.dp))
                        Text("Aprobar", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2E7D32), modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
        }
    }
}
