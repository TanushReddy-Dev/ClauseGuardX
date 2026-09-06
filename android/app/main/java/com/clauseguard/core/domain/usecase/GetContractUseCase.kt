package com.clauseguard.core.domain.usecase

import com.clauseguard.core.domain.model.Contract
import com.clauseguard.core.domain.repository.ContractRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.result.Result

/**
 * Use case for retrieving a contract from the local vault by ID.
 * Pure domain logic — no Android UI, no database imports, no Ktor.
 *
 * The repository interface abstracts the data source (SQLDelight), so this
 * use case remains completely interchangeable (Postgres, SQLite, remote API).
 */
class GetContractUseCase(
    private val repository: ContractRepository
) {

    /** Retrieve a single contract by ID from the local vault. */
    suspend fun getContract(contractId: String): Result<Contract> =
        repository.getContractFromVault(contractId).first()

    /** Retrieve all contracts as a Flow (emits each Contract as it becomes available). */
    fun getAllContracts(): Flow<Contract> =
        repository.getContractFromVault("0")  // placeholder; real implementation
                // would query all contracts from the database
                .distinct()