package com.tuapp.libreta.test

import com.tuapp.libreta.domain.model.*
import com.tuapp.libreta.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

// ── Fake Student Repository ───────────────────────────────────────────────────

class FakeStudentRepository : StudentRepository {
    val students = MutableStateFlow<List<Student>>(emptyList())

    override fun getStudentsByClass(classId: String): Flow<List<Student>> =
        students.map { it.filter { s -> s.classId == classId } }

    override suspend fun saveStudent(student: Student) {
        students.value = students.value.filterNot { it.id == student.id } + student
    }

    override suspend fun deleteStudent(id: String) {
        students.value = students.value.filterNot { it.id == id }
    }
}

// ── Fake Attendance Repository ────────────────────────────────────────────────

class FakeAttendanceRepository : AttendanceRepository {
    val records = MutableStateFlow<List<Attendance>>(emptyList())

    override fun getByStudent(studentId: String): Flow<List<Attendance>> =
        records.map { it.filter { a -> a.studentId == studentId } }

    override suspend fun save(attendance: Attendance) {
        records.value = records.value.filterNot { it.id == attendance.id } + attendance
    }

    override suspend fun delete(id: String) {
        records.value = records.value.filterNot { it.id == id }
    }
}

// ── Fake Justification Repository ────────────────────────────────────────────

class FakeJustificationRepository : JustificationRepository {
    val justifications = MutableStateFlow<List<Justification>>(emptyList())

    override fun getByStudent(studentId: String): Flow<List<Justification>> =
        justifications.map { it.filter { j -> j.studentId == studentId } }

    override suspend fun save(justification: Justification) {
        // Replace if same ID exists, otherwise append
        val existing = justifications.value.indexOfFirst { it.id == justification.id }
        justifications.value = if (existing >= 0) {
            justifications.value.toMutableList().also { it[existing] = justification }
        } else {
            justifications.value + justification
        }
    }

    override suspend fun delete(id: String) {
        justifications.value = justifications.value.filterNot { it.id == id }
    }
}

// ── Fake Message Repository ───────────────────────────────────────────────────

class FakeMessageRepository : MessageRepository {
    val messages = MutableStateFlow<List<Message>>(emptyList())

    override fun getByReceiver(receiverId: String): Flow<List<Message>> =
        messages.map { it.filter { m -> m.receiverId == receiverId } }

    override suspend fun save(message: Message) {
        messages.value = messages.value + message
    }

    override suspend fun delete(id: String) {
        messages.value = messages.value.filterNot { it.id == id }
    }
}
