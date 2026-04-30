package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.Grade
import com.tuapp.libreta.domain.model.SubjectAverage
import com.tuapp.libreta.domain.repository.GradeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetStudentGradesUseCase(private val repository: GradeRepository) {
    operator fun invoke(studentId: UuidString): Flow<List<SubjectAverage>> =
        repository.getByStudent(studentId).map { grades ->
            grades.groupBy { it.subject ?: "General" }
                .map { (subject, subjectGrades) ->
                    val totalWeight = subjectGrades.sumOf { it.weight }
                    val avg = if (totalWeight <= 0) 0.0 
                             else subjectGrades.sumOf { it.score * it.weight } / totalWeight
                    
                    // Redondear a 1 decimal (ej: 6.24 -> 6.2)
                    val roundedAvg = (avg * 10).toInt() / 10.0
                    
                    SubjectAverage(subject, roundedAvg, subjectGrades.sortedByDescending { it.date })
                }
                .sortedBy { it.subject }
        }
}

class SaveGradeUseCase(private val repository: GradeRepository) {
    suspend operator fun invoke(grade: Grade): Result<Unit> = runCatching {
        // Reglas de Negocio: Validaciones
        if (grade.score < 1.0 || grade.score > 7.0) {
            throw IllegalArgumentException("La nota debe estar entre 1.0 y 7.0")
        }
        if (grade.weight <= 0) {
            throw IllegalArgumentException("El peso de la evaluación debe ser mayor a 0")
        }
        if (grade.title.isBlank()) {
            throw IllegalArgumentException("El título de la evaluación no puede estar vacío")
        }
        
        repository.save(grade)
    }
}

class DeleteGradeUseCase(private val repository: GradeRepository) {
    suspend operator fun invoke(id: UuidString) = repository.delete(id)
}
