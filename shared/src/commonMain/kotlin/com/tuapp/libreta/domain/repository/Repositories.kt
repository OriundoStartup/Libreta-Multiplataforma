package com.tuapp.libreta.domain.repository

import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.ClassRoom
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.model.Profile
import com.tuapp.libreta.domain.model.Student
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getAll(): Flow<List<Profile>>
    suspend fun save(profile: Profile)
    suspend fun delete(id: String)
}

interface ClassRoomRepository {
    fun getAll(): Flow<List<ClassRoom>>
    suspend fun save(classRoom: ClassRoom)
    suspend fun delete(id: String)
}

interface StudentRepository {
    fun getStudentsByClass(classId: String): Flow<List<Student>>
    suspend fun saveStudent(student: Student)
    suspend fun deleteStudent(id: String)   // soft-delete → PENDING_DELETE
}

interface AttendanceRepository {
    fun getByStudent(studentId: String): Flow<List<Attendance>>
    suspend fun save(attendance: Attendance)
    suspend fun delete(id: String)
}

interface JustificationRepository {
    fun getByStudent(studentId: String): Flow<List<Justification>>
    suspend fun save(justification: Justification)
    suspend fun delete(id: String)
}

interface MessageRepository {
    fun getByReceiver(receiverId: String): Flow<List<Message>>
    suspend fun save(message: Message)
    suspend fun delete(id: String)
}
