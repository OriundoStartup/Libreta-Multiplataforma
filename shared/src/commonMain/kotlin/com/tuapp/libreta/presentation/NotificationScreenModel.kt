package com.tuapp.libreta.presentation

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.model.JustificationStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.JustificationRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val title: String,
    val content: String,
    val date: String,
    val isRead: Boolean = false,
    val associatedId: String? = null,
    val studentId: String? = null
)

enum class NotificationType { MESSAGE, ATTENDANCE, JUSTIFICATION, NOTICE, ANNOTATION }

sealed interface NotificationUiState {
    data object Loading : NotificationUiState
    data class Success(val notifications: List<NotificationItem>) : NotificationUiState
    data class Error(val message: String) : NotificationUiState
}

class NotificationScreenModel(
    private val messageRepo: MessageRepository,
    private val attendanceRepo: AttendanceRepository,
    private val justificationRepo: JustificationRepository
) : ScreenModel {

    private val _state = MutableStateFlow<NotificationUiState>(NotificationUiState.Loading)
    val state: StateFlow<NotificationUiState> = _state.asStateFlow()

    fun load(parentId: String, studentIds: List<UuidString>) {
        screenModelScope.launch {
            _state.value = NotificationUiState.Loading
            try {
                val notifications = mutableListOf<NotificationItem>()

                // 1. Mensajes del inbox
                val inbox = messageRepo.getInbox(parentId)
                notifications.addAll(inbox.map { thread ->
                    val isAnnotation = thread.lastMessage.startsWith("[🌟") || thread.lastMessage.startsWith("[⚠️")
                    NotificationItem(
                        id = "msg-${thread.contactId}",
                        type = if (isAnnotation) NotificationType.ANNOTATION else NotificationType.MESSAGE,
                        title = if (isAnnotation) "Nueva Anotación Registrada" else "Mensaje de ${thread.contactName}",
                        content = thread.lastMessage,
                        date = if (thread.unread) "Sin leer" else "Leído",
                        isRead = !thread.unread,
                        associatedId = thread.contactId.value
                    )
                })

                // 2. Por cada estudiante, traer asistencia y justificaciones recientes
                studentIds.forEach { sid ->
                    val attendance = attendanceRepo.getByStudent(sid).first().take(5)
                    notifications.addAll(attendance.mapIndexed { i, att ->
                        NotificationItem(
                            id = "att-${sid.value}-$i",
                            type = NotificationType.ATTENDANCE,
                            title = "Asistencia: ${if (att.status == AttendanceStatus.PRESENT) "Presente" else "Ausente"}",
                            content = "Registro del día ${att.date}",
                            date = att.date,
                            isRead = true,
                            studentId = sid.value
                        )
                    })

                    val justifications = justificationRepo.getByStudent(sid).first().take(5)
                    notifications.addAll(justifications.map { just ->
                        NotificationItem(
                            id = "just-${just.id?.value}",
                            type = NotificationType.JUSTIFICATION,
                            title = "Trámite: ${just.reason}",
                            content = "Estado: ${just.status.name}",
                            date = just.date.toString(),
                            isRead = just.status != JustificationStatus.PENDING,
                            studentId = sid.value
                        )
                    })
                }

                _state.value = NotificationUiState.Success(notifications.sortedBy { it.id }) // Simplificado
            } catch (e: Exception) {
                _state.value = NotificationUiState.Error(e.message ?: "Error al cargar notificaciones")
            }
        }
    }
}
