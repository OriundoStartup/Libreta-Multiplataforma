package com.tuapp.libreta.domain.repository

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getAll(): Flow<List<Profile>>
    suspend fun save(profile: Profile)
    suspend fun delete(id: UuidString)
}

interface ClassRoomRepository {
    fun getAll(): Flow<List<ClassRoom>>
    suspend fun save(classRoom: ClassRoom)
    suspend fun delete(id: UuidString)
}

interface StudentRepository {
    fun getStudentsByClass(classId: UuidString): Flow<List<Student>>
    fun getStudentsByParent(parentId: UuidString): Flow<List<Student>>
    suspend fun saveStudent(student: Student)
    suspend fun deleteStudent(id: UuidString)
}

interface AttendanceRepository {
    fun getByStudent(studentId: UuidString): Flow<List<Attendance>>
    suspend fun save(attendance: Attendance)
    suspend fun delete(id: UuidString)
}

interface JustificationRepository {
    fun getByStudent(studentId: UuidString): Flow<List<Justification>>
    suspend fun save(justification: Justification)
    suspend fun delete(id: UuidString)
}

interface MessageRepository {
    fun getByReceiver(receiverId: UuidString): Flow<List<Message>>
    suspend fun save(message: Message)
    suspend fun delete(id: UuidString)
}

interface InvitationCodeRepository {
    suspend fun generate(studentId: UuidString, teacherId: UuidString): InvitationCode
    suspend fun claim(code: String, parentId: UuidString): Result<InvitationCode>
    fun getByTeacher(teacherId: UuidString): Flow<List<InvitationCode>>
}

interface CourseAssignmentRepository {
    fun getByTeacher(teacherId: UuidString): Flow<List<CourseAssignment>>
    suspend fun assign(assignment: CourseAssignment)
    suspend fun generateColleagueInvite(courseId: UuidString, schoolId: UuidString, issuedByTeacherId: UuidString): String
}

interface SchoolRepository {
    fun getByTeacher(teacherId: UuidString): Flow<List<School>>
}

interface CommunicationRepository {
    suspend fun sendGeneralNotice(senderId: UuidString, classId: UuidString, content: String)
    fun getByClass(classId: UuidString): Flow<List<Message>>
}
