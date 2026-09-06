package com.clauseguard.core.domain.usecase

import com.clauseguard.core.domain.model.Contract
import com.clauseguard.core.domain.repository.ContractRepository
import kotlinx.coroutines.result.Result

class AnalyzeContractUseCase(
    private val repository: ContractRepository
) {

    /** Convenience operator delegate: invoke(fileBytes, filename) → Result<Contract> */
    suspend operator fun invoke(fileBytes: ByteArray, filename: String): Result<Contract> =
        repository.analyzeContract(fileBytes, filename)
}