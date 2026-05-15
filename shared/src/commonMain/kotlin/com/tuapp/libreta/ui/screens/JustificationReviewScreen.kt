package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.tuapp.libreta.presentation.JustificationReviewState
import com.tuapp.libreta.presentation.JustificationScreenModel
import com.tuapp.libreta.ui.components.ShimmerCard
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class JustificationReviewScreen(
    val classId: String,
    val parentId: String   // para enviar notificación silenciosa
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: JustificationScreenModel = koinScreenModel()
        val state by model.reviewState.collectAsState()

        LaunchedEffect(classId) { model.loadPending(classId) }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = { IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }},
                    title  = { Text("Justificaciones Pendientes",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) { padding ->
            when (val s = state) {
                JustificationReviewState.Loading -> LazyColumn(
                    contentPadding      = PaddingValues(16.dp).plus(padding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled   = false
                ) { items(4) { ShimmerCard() } }

                JustificationReviewState.Empty -> Box(
                    Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📋", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("Sin justificaciones pendientes",
                            style = MaterialTheme.typography.titleMedium)
                    }
                }

                is JustificationReviewState.Success -> LazyColumn(
                    contentPadding      = PaddingValues(16.dp).plus(padding),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(s.pending, key = { it.id?.value ?: it.hashCode() }) { justification ->
                        JustificationCard(
                            justification   = justification,
                            onApprove       = { model.approve(justification, parentId) },
                            onReject        = { model.reject(justification, parentId) },
                            onGetSignedUrl  = { model.getSignedUrl(it) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun JustificationCard(
    justification: Justification,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onGetSignedUrl: suspend (String) -> String
) {
    val dt = Instant.fromEpochMilliseconds(justification.date)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    val dateStr = "${dt.day}/${dt.monthNumber}/${dt.year}"

    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Text(
                        text     = dateStr,
                        style    = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color    = Color(0xFFE65100),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text  = justification.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!justification.documentUrl.isNullOrBlank()) {
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                OutlinedButton(
                    onClick = { 
                        scope.launch {
                            val url = onGetSignedUrl(justification.documentUrl)
                            if (url.isNotBlank()) {
                                com.tuapp.libreta.ui.util.openUrl(url)
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add, 
                        contentDescription = null,
                        modifier = Modifier.size(18.dp).padding(end = 8.dp)
                    )
                    Text("Ver Certificado Adjunto", style = MaterialTheme.typography.labelLarge)
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Rechazar
                OutlinedButton(
                    onClick  = onReject,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828)),
                    border   = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFC62828))
                    )
                ) {
                    Icon(Icons.Default.Close, contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp))
                    Text("Rechazar")
                }
                // Aprobar
                Button(
                    onClick  = onApprove,
                    modifier = Modifier.weight(1f),
                    colors   = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Check, contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp))
                    Text("Aprobar")
                }
            }
        }
    }
}

// Helper
private fun PaddingValues.plus(other: PaddingValues): PaddingValues = PaddingValues(
    start  = calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + other.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    end    = calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)   + other.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    top    = calculateTopPadding()    + other.calculateTopPadding(),
    bottom = calculateBottomPadding() + other.calculateBottomPadding()
)
