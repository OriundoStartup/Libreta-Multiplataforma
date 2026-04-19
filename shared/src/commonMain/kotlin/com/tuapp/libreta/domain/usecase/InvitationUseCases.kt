package com.tuapp.libreta.domain.usecase

import com.tuapp.libreta.data.util.UuidString
import com.tuapp.libreta.domain.model.InvitationCode
import com.tuapp.libreta.domain.repository.InvitationCodeRepository
import kotlinx.coroutines.flow.Flow

class GenerateInvitationCodeUseCase(private val repo: InvitationCodeRepository) {
    suspend operator fun invoke(studentId: UuidString, teacherId: UuidString): InvitationCode =
        repo.generate(studentId, teacherId)
}

class ClaimInvitationCodeUseCase(private val repo: InvitationCodeRepository) {
    suspend operator fun invoke(code: String, parentId: UuidString): Result<InvitationCode> =
        repo.claim(code, parentId)
}

class GetTeacherInvitationsUseCase(private val repo: InvitationCodeRepository) {
    operator fun invoke(teacherId: UuidString): Flow<List<InvitationCode>> =
        repo.getByTeacher(teacherId)
}
