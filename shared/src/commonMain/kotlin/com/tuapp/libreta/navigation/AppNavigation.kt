package com.tuapp.libreta.navigation

import cafe.adriel.voyager.core.screen.Screen
import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Course
import com.tuapp.libreta.domain.model.UserRole
import com.tuapp.libreta.ui.screens.AttendanceHistoryScreen
import com.tuapp.libreta.ui.screens.AttendanceScreen
import com.tuapp.libreta.ui.screens.ComposeNoticeScreen
import com.tuapp.libreta.ui.screens.CourseEditScreen
import com.tuapp.libreta.ui.screens.CourseStatsScreen
import com.tuapp.libreta.ui.screens.EnrollmentScreen
import com.tuapp.libreta.ui.screens.JustificationReviewScreen
import com.tuapp.libreta.ui.screens.JustificationScreen
import com.tuapp.libreta.ui.screens.LoginScreen
import com.tuapp.libreta.ui.screens.MessageDetailScreen
import com.tuapp.libreta.ui.screens.MessageListScreen
import com.tuapp.libreta.ui.screens.NoticeListScreen
import com.tuapp.libreta.ui.screens.ParentDashboardScreen
import com.tuapp.libreta.ui.screens.ProfileScreen
import com.tuapp.libreta.ui.screens.StudentDetailScreen
import com.tuapp.libreta.ui.screens.StudentListScreen
import com.tuapp.libreta.ui.screens.TeacherDashboardScreen
import com.tuapp.libreta.ui.screens.JustificationListScreen
import com.tuapp.libreta.ui.screens.NotificationScreen
import com.tuapp.libreta.ui.screens.ParentStudentDetailScreen
import com.tuapp.libreta.ui.screens.AttendanceReportScreen
import com.tuapp.libreta.ui.screens.GlobalJustificationReviewScreen
import com.tuapp.libreta.ui.screens.GradeScreen
import com.tuapp.libreta.ui.screens.ParentStudentDetailScreen as ParentStudentDetailScreenUI
import com.tuapp.libreta.ui.screens.NoticeListScreen as NoticeListScreenUI

object AppConfig {
    const val CURRENT_USER_ID = "user-current"
    const val DEMO_CLASS_ID   = "clase-demo"
    const val DEMO_CLASS_NAME = "4° Básico A"
    const val DEMO_STUDENT_ID = "student-demo"
    const val DEMO_PARENT_ID  = "parent-demo"
    const val DEMO_TEACHER_ID = "teacher-demo"
}

object AppNavigation {

    fun startDestination(): Screen = LoginScreen

    fun initialScreen(role: UserRole = UserRole.TEACHER, userId: String = AppConfig.DEMO_PARENT_ID): Screen = when (role) {
        UserRole.TEACHER -> teacherDashboard()
        UserRole.PARENT  -> parentDashboard(userId)
    }

    fun teacherDashboard(): Screen = TeacherDashboardScreen

    fun parentDashboard(userId: String = AppConfig.DEMO_PARENT_ID): Screen =
        ParentDashboardScreen(parentId = userId)

    fun messages(): Screen = MessageListScreen

    fun newMessage(): Screen = composeNotice()

    fun messageDetail(contactId: UuidString, contactName: String, contextLabel: String? = null): Screen =
        MessageDetailScreen(contactId = contactId, contactName = contactName, contextLabel = contextLabel)

    fun justificationForm(parentId: String = AppConfig.CURRENT_USER_ID, studentId: String = AppConfig.DEMO_STUDENT_ID): Screen =
        JustificationScreen(
            studentId = studentId,
            parentId  = parentId,
            teacherId = AppConfig.DEMO_TEACHER_ID
        )

    fun justificationList(studentId: String): Screen = JustificationListScreen(studentId)

    fun notificationScreen(parentId: String, studentIds: List<String>): Screen = 
        NotificationScreen(parentId, studentIds)

    fun parentStudentDetail(studentId: String, name: String, rut: String?): Screen = 
        ParentStudentDetailScreen(studentId, name, rut)

    fun attendanceReport(courseId: String, courseName: String): Screen = 
        AttendanceReportScreen(courseId, courseName)

    fun globalJustificationReview(): Screen = GlobalJustificationReviewScreen

    fun justificationReview(classId: String = AppConfig.DEMO_CLASS_ID): Screen =
        JustificationReviewScreen(classId = classId, parentId = AppConfig.DEMO_PARENT_ID)

    fun courseStats(classId: String = AppConfig.DEMO_CLASS_ID, className: String = AppConfig.DEMO_CLASS_NAME): Screen =
        CourseStatsScreen(classId = classId, className = className)

    fun composeNotice(classId: String? = null, studentId: String? = null, className: String? = null): Screen = 
        ComposeNoticeScreen(preselectedClassId = classId, preselectedStudentId = studentId, preselectedClassName = className)

    fun noticeList(classId: UuidString): Screen = NoticeListScreen(classId = classId)

    fun attendance(courseId: String, courseName: String): Screen = 
        AttendanceScreen(courseId = courseId, courseName = courseName)

    fun profile(): Screen = ProfileScreen

    fun studentList(classId: String): Screen = StudentListScreen(classId = classId)

    fun attendanceHistory(studentId: String, studentName: String): Screen =
        AttendanceHistoryScreen(studentId = studentId, studentName = studentName)

    fun studentDetail(studentId: String, studentName: String, courseId: String, parentId: String): Screen =
        StudentDetailScreen(studentId = studentId, studentName = studentName, courseId = courseId, parentId = parentId)

    fun enrollment(): Screen = EnrollmentScreen

    fun studentGrades(studentId: String, studentName: String, courseId: String, isTeacher: Boolean = false): Screen =
        GradeScreen(studentId, studentName, courseId, isTeacher)

    fun courseEdit(courseId: String, courseName: String, course: Course): Screen =
        CourseEditScreen(courseId = courseId, courseName = courseName, course = course)
}
