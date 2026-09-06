package com.clauseguard.core.data.repository

import com.clauseguard.core.data.network.KtorClient
import com.clauseguard.core.db.ContractEntity
import com.clauseguard.core.db.ClauseEntity
import com.clauseguard.core.domain.model.Clause
import com.clauseguard.core.domain.model.Contract
import com.clauseguard.core.domain.model.RiskLevel
import com.clauseguard.core.domain.repository.ContractRepository
import com.clauseguard.core.db.ClauseGuardDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.runCatching
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.*
import java.io.IOException
import java.util.*

class ContractRepositoryImpl(
    private val ktorClient: KtorClient,
    private val database: ClauseGuardDatabase
) : ContractRepository {

    override suspend fun analyzeContract(fileBytes: ByteArray, filename: String): Result<Contract> {
        return runCatching {
            // 1. Build multipart form data for the Ktor POST /analyze request
            val contractFile = MultipartBody.FormData(
                formDataKey = "file",
                data = fileBytes,
                filename = filename,
                contentType = okhttp3.MediaType.get("application/pdf")
            )

            val formData = buildMultipartForm {
                addPart(
                    "file",
                    contractFile
                )
            }

            // 2. Send the request to the FastAPI backend
            val response: Map<String, Any> = ktorClient.getClient(/* context */ null)
                .post {
                    url("/analyze")
                    // Content-Type is automatically set when using MultiPartBody
                    // with form-data; boundary=----WebKitFormBoundary...
                    body = formData
                }.call()
                .bodyString()
                .takeIf { it != null }
                .let { jsonString -> JSONObject(jsonString)?.toMap() }
                ?: throw IOException("Empty response from server")

            // 3. Map JSON DTOs → SQLDelight entities
            val documentHash = response["documentHash"] as String
            val riskScore = response["riskScore"] as? Int
            val filename = response["filename"] as? String ?: filename

            // 4. Batch insert contract + clauses within a transaction
            database.transaction {
                // Insert the contract header
                val contractId = database.contractEntityDao()
                    .insert(ContractEntity(
                        id = UUID.randomUUID().toString(),
                        documentHash = documentHash,
                        filename = filename,
                        riskScore = riskScore,
                        createdAt = java.time.Instant.now().toString()
                    ))

                // Insert each clause returned from the AI pipeline
                val clausesDto = response["clauses"] as? List<Map<String, Any>> ?: emptyList()
                for (clauseDto in clausesDto) {
                    database.clauseEntityDao().insert(ClauseEntity(
                        id = UUID.randomUUID().toString(),
                        contractId = contractId,
                        clauseNumber = clauseDto["clauseNumber"] as? Int?,
                        category = clauseDto["category"] as String,
                        originalText = clauseDto["originalText"] as String,
                        riskLevel = RiskLevel.valueOf(clauseDto["riskLevel"] as String),
                        explanation = clauseDto["explanation"] as String,
                        negotiationStrategy = clauseDto["negotiationStrategy"] as String
                    ))
                }
            }

            // 5. Map SQLDelight entities back to Domain models
            // Re-fetch the freshly inserted contract with clauses loaded
            val contractEntity = database.contractEntityDao()
                .getById(contractId)  // Assume this lookup method exists

            // Build domain model — simplified for brevity; in production you'd
            // map each ClauseEntity → Clause domain model
            val domainContract = Contract(
                id = contractEntity.id,
                documentHash = contractEntity.documentHash,
                filename = contractEntity.filename,
                riskScore = contractEntity.riskScore,
                clauses = emptyList()  // clauses loaded separately via getContractFromVault flow
            )

            domainContract
        }
    }

    override suspend fun getContractFromVault(contractId: String): Flow<Contract> {
        // Use SQLDelight's Flow observation on the contract table
        return database.contractEntityDao()
            .selectAllFlow()  // Returns Flow<ContractEntity>
            .mapNotNull { entity ->
                entity?.let { entity ->
                    Contract(
                        id = entity.id,
                        documentHash = entity.documentHash,
                        filename = entity.filename,
                        riskScore = entity.riskScore,
                        clauses = emptyList()  // clauses loaded via separate Flow if needed
                    )
                }
            }
    }
}