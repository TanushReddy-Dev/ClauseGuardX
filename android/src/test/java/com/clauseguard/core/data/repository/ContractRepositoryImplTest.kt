package com.clauseguard.core.data.repository

import com.clauseguard.core.db.ClauseGuardDatabase
import com.clauseguard.core.domain.model.Contract
import com.clauseguard.core.domain.model.RiskLevel
import com.clauseguard.core.domain.repository.ContractRepository
import com.clauseguard.core.db.ContractEntity
import com.clauseguard.core.db.ClauseEntity
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockWebServer
import okhttp3.Request
import okhttp3.RequestBody
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.RoboTest
import org.robolectric.runners.RobolectricTestRunner
import kotlinx.coroutines.runTest
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import java.sql.Timestamp
import java.time.Instant

@RunWith(RobolectricTestRunner::class)
class ContractRepositoryImplTest {

    private lateinit var repository: ContractRepositoryImpl
    private lateinit var mockWebServer: MockWebServer
    private lateinit var db: ClauseGuardDatabase

    @Before
    fun setup() {
        // In-memory SQLDelight database
        db = ClauseGuardDatabase.create(
            "jdbc:sqlite::memory:",
            driverName = "com.zeroc.dev.h2.h2driver.H2Driver"
        )

        // MockWebServer for Ktor HTTP testing
        mockWebServer = MockWebServer()
        mockWebServer.start()

        // Intercept POST /analyze with a static JSON response matching AnalysisResult schema
        val staticResponse = """
        {
            "document_hash": "d4e1d7f0e7a7e4c3b2a1f6d5c4a3b2d1e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0",
            "risk_score": 5,
            "filename": "test.pdf",
            "clauses": [
                {
                    "id": "c1d2e3f4-5678-90ab-cdef-1234567890ab",
                    "category": "Test",
                    "original_text": "dummy text",
                    "risk_level": "LOW",
                    "explanation": "Test clause",
                    "negotiation_strategy": "Test strategy",
                    "clause_number": 1
                }
            ]
        }
        """.trimIndent()

        val dispatcher = object : Dispatcher {
            override fun dispatch(request: okhttp3.Request): okhttp3.Response {
                if (request.method() == "POST" && request.path().contains("/analyze")) {
                    return okhttp3.Response.Builder()
                        .code(201)
                        .body(staticResponse)
                        .build()
                }
                return okhttp3.Response.Builder().code(404).build()
            }
        }
        mockWebServer.dispatcher = dispatcher

        // Initialize Repository with mocked dependencies
        repository = ContractRepositoryImpl(
            ktorClient = KtorClient(),  // Will use MockEngine internally
            database = db
        )
    }

    @After
    fun teardown() {
        mockWebServer.stop()
        db.close()
    }

    @Test
    fun testAnalyzeContractSuccessfullySavesToDatabase() = runTest {
        // 1. Arrange: raw PDF bytes and filename
        val fileBytes = "%PDF-1.4 %âãÏÓ 0 0 0 0 R".toByteArray()
        val filename = "test.pdf"

        // 2. Act: Call the repository method
        val result = repository.analyzeContract(fileBytes, filename)

        // 3. Assert: Result should be a success
        assert(result.isSuccess) { "Expected success but got: ${result.exceptionOrNull()" }

        // 4. Assert: Database should have the contract and clauses inserted
        val contractEntity = db.contractEntityDao().getById("test-id")
        assert(contractEntity != null) { "ContractEntity should exist in DB" }

        val clauseEntity = db.clauseEntityDao().selectAllFlow().firstOrNull()
        assert(clauseEntity != null) { "ClauseEntity should exist in DB" }

        // 5. Assert: ContractEntity fields
        assert(contractEntity.documentHash != null) { "documentHash should not be null" }
        assert(contractEntity.filename == "test.pdf") { "filename should match" }

        // 6. Assert: ClauseEntity fields
        assert(clauseEntity.category == "Test") { "category should match" }
        assert(clauseEntity.riskLevel == "LOW") { "riskLevel should match" }
        assert(clauseEntity.originalText == "dummy text") { "originalText should match" }
    }
}