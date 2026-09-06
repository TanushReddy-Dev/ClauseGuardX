package com.clauseguard.core.domain.usecase

import com.clauseguard.core.domain.model.Contract
import kotlinx.coroutines.flow.Flow

/**
 * Use case for searching contracts by various criteria.
 *
 * This is a pure domain abstraction — the actual filtering logic
 * and data source querying are implemented in the Data layer's
 * repository, but the use case defines the contract (no pun intended)
 * for what search parameters and return types look like.
 */
class SearchContractsUseCase {

    /** Search contracts by risk level. */
    fun searchByRiskLevel(riskLevel: com.clauseguard.core.domain.model.RiskLevel): Flow<Contract> {
        // Domain-level filtering; the repository implements the actual Flow
        // This is a skeleton — concrete implementation depends on the data source
        throw UnsupportedOperationException("Implement repository-level search")
    }

    /** Search contracts by filename substring. */
    fun searchByFilename(query: String): Flow<Contract> {
        throw UnsupportedOperationException("Implement repository-level search")
    }