package com.tuapp.libreta.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.presentation.ConversationUiState
import com.tuapp.libreta.presentation.MessageScreenModel

data class MessageDetailScreen(
    val contactId: UuidString,
    val contactName: String,
    val contextLabel: String? = null
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model: MessageScreenModel = koinScreenModel()
        val conversationState by model.conversation.collectAsState()
        val sending by model.sending.collectAsState()
        var messageText by remember { mutableStateOf("") }
        val listState = rememberLazyListState()

        LaunchedEffect(Unit) {
            model.openConversation(contactId)
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                    },
                    title = {
                        Column {
                            Text(contactName, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            contextLabel?.let { 
                                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                )
            },
            bottomBar = {
                MessageInput(
                    value = messageText,
                    sending = sending,
                    onChange = { messageText = it },
                    onSend = {
                        if (messageText.isNotBlank()) {
                            model.sendMessage(contactId, messageText)
                            messageText = ""
                        }
                    }
                )
            }
        ) { padding ->
            when (val s = conversationState) {
                ConversationUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is ConversationUiState.Success -> {
                    val messages = s.messages
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
                    }

                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(16.dp, padding.calculateTopPadding() + 8.dp, 16.dp, padding.calculateBottomPadding() + 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(messages, key = { it.id?.value ?: it.hashCode() }) { message ->
                            ChatBubble(
                                message = message,
                                isMine = message.senderId.value == model.currentUserId?.value
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatBubble(message: Message, isMine: Boolean) {
    val isPositiveNote = message.content.startsWith("[🌟")
    val isNegativeNote = message.content.startsWith("[⚠️")
    val isAnnotation   = isPositiveNote || isNegativeNote

    val bubbleColor = when {
        isPositiveNote -> Color(0xFFFFFDE7) // Amarillo muy claro
        isNegativeNote -> Color(0xFFFFF3F3) // Rojo muy claro
        isMine         -> MaterialTheme.colorScheme.primary
        else           -> MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val textColor = when {
        isAnnotation -> MaterialTheme.colorScheme.onSurface
        isMine       -> MaterialTheme.colorScheme.onPrimary
        else         -> MaterialTheme.colorScheme.onSurface
    }

    val shape = if (isMine)
        RoundedCornerShape(16.dp, 4.dp, 16.dp, 16.dp)
    else
        RoundedCornerShape(4.dp, 16.dp, 16.dp, 16.dp)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = shape,
            color = bubbleColor,
            modifier = Modifier
                .widthIn(max = 280.dp)
                .then(
                    if (isAnnotation) Modifier.border(
                        1.dp, 
                        if (isPositiveNote) Color(0xFFFBC02D) else Color(0xFFD32F2F), 
                        shape
                    ) else Modifier
                )
        ) {
            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                if (isAnnotation) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                        Icon(
                            imageVector = if (isPositiveNote) Icons.Default.Stars else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (isPositiveNote) Color(0xFFFBC02D) else Color(0xFFD32F2F),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPositiveNote) "ANOTACIÓN POSITIVA" else "ANOTACIÓN NEGATIVA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                            color = if (isPositiveNote) Color(0xFFFBC02D) else Color(0xFFD32F2F)
                        )
                    }
                }
                
                Text(
                    text = if (isAnnotation) {
                        // Quitamos el tag del contenido para mostrarlo limpio
                        message.content.substringAfter("] ").trim()
                    } else message.content,
                    color = textColor,
                    style = if (isAnnotation) MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium) 
                            else MaterialTheme.typography.bodyMedium
                )
                
                message.createdAt?.let { time ->
                    Row(
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = com.tuapp.libreta.data.util.formatIsoToTime(time),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isMine) {
                            val isRead = message.readAt != null
                            Icon(
                                imageVector = if (isRead) Icons.Default.DoneAll else Icons.Default.Done,
                                contentDescription = if (isRead) "Leído" else "Enviado",
                                modifier = Modifier.size(14.dp),
                                tint = if (isRead) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageInput(
    value: String,
    sending: Boolean,
    onChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Escribe un mensaje...") },
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
            IconButton(
                onClick = onSend,
                enabled = value.isNotBlank() && !sending,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (sending) CircularProgressIndicator(Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar")
            }
        }
    }
}
