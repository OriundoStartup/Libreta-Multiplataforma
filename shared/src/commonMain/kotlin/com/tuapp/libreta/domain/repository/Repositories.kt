package com.tuapp.libreta.domain.repository

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Attendance
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.model.CourseAssignment
import com.tuapp.libreta.domain.model.Grade
import com.tuapp.libreta.domain.model.InvitationCode
import com.tuapp.libreta.domain.model.Justification
import com.tuapp.libreta.domain.model.Message
import com.tuapp.libreta.domain.model.Profile
import com.tuapp.libreta.domain.model.School
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.usecase.MessageThread
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getAll(): Flow<List<Profile>>
    suspend fun save(profile: Profile)
    suspend fun delete(id: UuidString)
}

interface ClassRoomRepository {
    fun getAll(): Flow<List<Course>>
    fun getByTeacher(teacherId: UuidString): Flow<List<Course>>
    suspend fun save(classRoom: Course)
    suspend fun delete(id: UuidString)
}

interface StudentRepository {
    fun getStudentsByClass(classId: UuidString): Flow<List<Student>>
    fun getStudentsByParent(parentId: UuidString): Flow<List<Student>>
    suspend fun saveStudent(student: Student)
    suspend fun updateStudentEnrollment(id: UuidString, name: String, rut: String?): Result<Unit>
    suspend fun deleteStudent(id: UuidString)
}

interface AttendanceRepository {
    fun getByStudent(studentId: UuidString): Flow<List<Attendance>>
    fun getByCourse(courseId: UuidString): Flow<List<Attendance>>
    suspend fun save(attendance: Attendance)
    suspend fun delete(id: UuidString)
}

interface JustificationRepository {
    fun getByStudent(studentId: UuidString): Flow<List<Justification>>
    fun getPendingByTeacher(teacherId: UuidString): Flow<List<Justification>>
    suspend fun save(justification: Justification)
    suspend fun saveWithAttachment(justification: Justification, fileBytes: ByteArray?, fileName: String?): Result<Unit>
    suspend fun delete(id: UuidString)
}

interface MessageRepository {
    suspend fun getInbox(currentUserId: String): List<MessageThread>
    suspend fun getConversation(currentUserId: String, contactId: String): List<Message>
    suspend fun sendMessage(receiverId: String, content: String): Result<Unit>
    suspend fun markAsRead(senderId: String, currentUserId: String)
    suspend fun save(message: Message)
    fun observeConversation(currentUserId: String, contactId: String): Flow<List<Message>>
    fun getInternalNotes(studentId: UuidString): Flow<List<Message>>
    suspend fun saveInternalNote(studentId: UuidString, senderId: UuidString, content: String): Result<Unit>
}

interface InvitationCodeRepository {
    suspend fun generate(studentId: UuidString, teacherId: UuidString): InvitationCode
    suspend fun claim(code: String, parentId: UuidString): Result<InvitationCode>
    fun getByTeacher(teacherId: UuidString): Flow<List<InvitationCode>>
}

interface CourseAssignmentRepository {
    fun getByTeacher(teacherId: UuidString): Flow<List<CourseAssignment>>
    suspend fun assign(assignment: CourseAssignment)
    suspend fun assignByCode(code: String, teacherId: UuidString): Result<Unit>
    suspend fun generateColleagueInvite(courseId: UuidString, schoolId: UuidString, issuedByTeacherId: UuidString): String
}

interface SchoolRepository {
    fun getByTeacher(teacherId: UuidString): Flow<List<School>>
}

interface CommunicationRepository {
    suspend fun sendGeneralNotice(senderId: UuidString, classId: UuidString, content: String)
    fun getByClass(classId: UuidString): Flow<List<Message>>
}

interface GradeRepository {
    fun getByStudent(studentId: UuidString): Flow<List<Grade>>
    fun getByCourse(courseId: UuidString): Flow<List<Grade>>
    suspend fun save(grade: Grade)
    suspend fun delete(id: UuidString)
}
