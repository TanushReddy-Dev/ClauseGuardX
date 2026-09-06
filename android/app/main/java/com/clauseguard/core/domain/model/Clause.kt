package com.clauseguard.core.domain.model

enum class RiskLevel {
    LOW,
    MEDIUM,
    HIGH
}

data class Clause(
    val id: String,
    val category: String,
    val originalText: String,
    val riskLevel: RiskLevel,
    val explanation: String,
    val negotiationStrategy: String
)