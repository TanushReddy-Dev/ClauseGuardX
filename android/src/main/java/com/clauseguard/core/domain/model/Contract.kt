package com.clauseguard.core.domain.model

data class Contract(
    val id: String,
    val documentHash: String,
    val filename: String,
    val riskScore: Int?,
    val clauses: List<Clause>
)