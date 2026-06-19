package com.tuapp.libreta.presentation

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.AttendanceStatus
import com.tuapp.libreta.domain.repository.AttendanceRepository
import com.tuapp.libreta.domain.repository.JustificationRepository
import com.tuapp.libreta.domain.repository.MessageRepository
import com.tuapp.libreta.domain.usecase.MessageThread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationScreenModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeMessageRepo = object : MessageRepository {
        override suspend fun getInbox(currentUserId: String): List<MessageThread> = listOf(
            MessageThread(contactId = UuidString("00000000-0000-0000-0000-000000000021"), contactName = "Profesor", lastMessage = "Hola", unread = true)
        )
        // ... otros métodos no usados en load()
        override suspend fun getConversation(u: String, c: String) = emptyList<com.tuapp.libreta.domain.model.Message>()
        override suspend fun sendMessage(r: String, c: String) = Result.success(Unit)
        override suspend fun markAsRead(s: String, c: String) {}
        override suspend fun save(m: com.tuapp.libreta.domain.model.Message) {}
        override fun observeConversation(u: String, c: String) = flowOf(emptyList<com.tuapp.libreta.domain.model.Message>())
        override fun getInternalNotes(s: UuidString) = flowOf(emptyList<com.tuapp.libreta.domain.model.Message>())
        override suspend fun saveInternalNote(s: UuidString, sn: UuidString, c: String) = Result.success(Unit)
    }

    private val fakeAttendanceRepo = object : AttendanceRepository {
        override fun getByStudent(studentId: UuidString) = flowOf(
            listOf(Attendance(id = UuidString("00000000-0000-0000-0000-0000000000a1"), studentId = studentId, date = "2024-01-01", status = AttendanceStatus.ABSENT))
        )
        override fun getByCourse(courseId: UuidString) = flowOf(emptyList<Attendance>())
        override suspend fun save(a: Attendance) {}
        override suspend fun delete(id: UuidString) {}
    }

    private val fakeJustificationRepo = object : JustificationRepository {
        override fun getByStudent(sid: UuidString) = flowOf(emptyList<com.tuapp.libreta.domain.model.Justification>())
        override fun getPendingByTeacher(tid: UuidString) = flowOf(emptyList<com.tuapp.libreta.domain.model.Justification>())
        override suspend fun save(j: com.tuapp.libreta.domain.model.Justification) {}
        override suspend fun saveWithAttachment(j: com.tuapp.libreta.domain.model.Justification, f: ByteArray?, n: String?) = Result.success(Unit)
        override suspend fun getAttachmentUrl(path: String): String = ""
        override suspend fun delete(id: UuidString) {}
    }

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `load fetches all notification types`() = runTest(testDispatcher) {
        val model = NotificationScreenModel(fakeMessageRepo, fakeAttendanceRepo, fakeJustificationRepo)
        model.load("00000000-0000-0000-0000-000000000011", listOf(UuidString("00000000-0000-0000-0000-000000000001")))
        
        val state = model.state.first { it is NotificationUiState.Success }
        val notifications = (state as NotificationUiState.Success).notifications
        
        assertTrue(notifications.any { it.type == NotificationType.MESSAGE }, "Debe haber al menos 1 mensaje")
        assertTrue(notifications.any { it.type == NotificationType.ATTENDANCE }, "Debe haber al menos 1 registro de asistencia")
    }
}
