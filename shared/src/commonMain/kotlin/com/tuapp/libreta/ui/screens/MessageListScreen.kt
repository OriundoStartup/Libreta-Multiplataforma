package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.tuapp.libreta.domain.usecase.MessageThread
import com.tuapp.libreta.navigation.AppNavigation
import com.tuapp.libreta.presentation.InboxUiState
import com.tuapp.libreta.presentation.MessageScreenModel
import com.tuapp.libreta.ui.components.ShimmerCard

object MessageListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: MessageScreenModel = koinScreenModel()
        val state by model.inbox.collectAsState()

        LaunchedEffect(Unit) { model.loadInbox() }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    title  = { Text("Mensajes", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest
        ) { padding ->
            when (val s = state) {
                InboxUiState.Loading -> LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp).plus(padding),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    userScrollEnabled = false
                ) { items(6) { ShimmerCard() } }

                InboxUiState.Empty -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💬", style = MaterialTheme.typography.displayMedium)
                        Spacer(Modifier.height(12.dp))
                        Text("Sin mensajes aún", style = MaterialTheme.typography.titleMedium)
                    }
                }

                is InboxUiState.Success -> LazyColumn(
                    contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp).plus(padding),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(s.threads, key = { it.contactId }) { thread ->
                        ThreadRow(thread = thread, onClick = {
                            navigator.push(AppNavigation.messageDetail(
                                contactId = thread.contactId,
                                contactName = thread.contactName
                            ))
                        })
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadRow(thread: MessageThread, onClick: () -> Unit) {
    Row(
        modifier              = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar
        Box(
            modifier         = Modifier.size(48.dp).clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text  = thread.contactName.first().uppercaseChar().toString(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }

        // Nombre + último mensaje
        Column(Modifier.weight(1f)) {
            Text(
                text  = thread.contactName,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (thread.unread) FontWeight.Bold else FontWeight.Normal
                )
            )
            Text(
                text     = thread.lastMessage,
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }

        // Indicador no leído
        if (thread.unread) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
        }
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
}

// Helper para combinar PaddingValues
private fun PaddingValues.plus(other: PaddingValues): PaddingValues = PaddingValues(
    start  = calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr) + other.calculateStartPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    end    = calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr)   + other.calculateEndPadding(androidx.compose.ui.unit.LayoutDirection.Ltr),
    top    = calculateTopPadding()    + other.calculateTopPadding(),
    bottom = calculateBottomPadding() + other.calculateBottomPadding()
)
