package com.clauseguard.core.domain.repository

import com.clauseguard.core.domain.model.Contract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.result.Result

interface ContractRepository {
    /** Analyze a contract from raw file bytes, returning a Contract with classified clauses. */
    suspend fun analyzeContract(fileBytes: ByteArray, filename: String): Result<Contract>

    /** Retrieve a contract from the local vault as a Flow of Contract objects. */
    suspend fun getContractFromVault(contractId: String): Flow<Contract>
}