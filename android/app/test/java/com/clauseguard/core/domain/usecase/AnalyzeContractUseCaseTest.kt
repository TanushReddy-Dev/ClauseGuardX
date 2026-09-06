package com.clauseguard.core.domain.usecase

import com.clauseguard.core.domain.model.Contract
import com.clauseguard.core.domain.model.RiskLevel
import com.clauseguard.core.domain.usecase.AnalyzeContractUseCase
import com.clauseguard.core.domain.repository.ContractRepository
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.RoboTest
import org.robolectric.runners.RobolectricTestRunner
import org.mockito.kotlin.argumentCaptors
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class AnalyzeContractUseCaseTest {

    @Test
    fun testUseCase_forwardsBytesAndHandlesResult() = runTest {
        // 1. Arrange: Mock the ContractRepository
        val mockRepository = mock(ContractRepository::class.java)

        // Define the mock behavior: analyzeContract returns a success Result with a Contract
        val mockContract = Contract(
            id = "test-contract-id",
            documentHash = "abc123def456ghi789jkl012mno345pqr678stu901v",
            filename = "test.pdf",
            riskScore = 5,
            clauses = emptyList()
        )

        `mock`<ContractRepository> {
            whenever(mockRepository.analyzeContract(any(), any())) {
                return Result.success(mockContract)
            }
        }

        val useCase = AnalyzeContractUseCase(repository = mockRepository)

        // 2. Act: Call the use case invoke operator
        val result = useCase(byteArrayOf(0xFE, 0xED, 0xFA, 0xCE), "test.pdf")

        // 3. Assert: Result should be success and contain the expected Contract
        assert(result.isSuccess) { "Expected success Result but got failure" }
        val contracts = result.getOrNull()
        assertTrue(::isValidUUID) { "Contract ID should be a valid UUID: ${contracts?.id}" }
        assert(contracts?.filename == "test.pdf") { "Filename should match" }
        assert(contracts?.riskScore == 5) { "Risk score should be 5" }

        // 4. Assert: The repository's analyzeContract was called with the correct args
        val captorFileBytes = argumentCaptor<ByteArray>()
        val captorFilename = argumentCaptor<String>()
        verify(mockRepository).analyzeContract(captorFileBytes, captorFilename)
        assert(captorFileBytes.first() == byteArrayOf(0xFE, 0xED, 0xFA, 0xCE)) { "Byte array should match" }
        assert(captorFilename.first() == "test.pdf") { "Filename should match" }
    }

    private fun isValidUUID(uuid: String): Boolean {
        try {
            java.util.UUID.fromString(uuid)
            return true
        } catch (e: IllegalArgumentException) {
            return false
        }
    }
}