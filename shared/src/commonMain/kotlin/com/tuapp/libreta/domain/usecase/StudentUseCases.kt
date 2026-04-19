package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Student
import com.tuapp.libreta.domain.repository.StudentRepository
import kotlinx.coroutines.flow.Flow

class GetStudentsByClassUseCase(private val repository: StudentRepository) {
    operator fun invoke(classId: UuidString): Flow<List<Student>> =
        repository.getStudentsByClass(classId)
}

class DeleteStudentUseCase(private val repository: StudentRepository) {
    suspend operator fun invoke(id: UuidString) = repository.deleteStudent(id)
}
