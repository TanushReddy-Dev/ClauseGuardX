package com.clauseguard.core

import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.clauseguard.core.data.network.KtorClient
import com.clauseguard.core.data.repository.ContractRepositoryImpl
import com.clauseguard.core.db.ClauseGuardDatabase
import com.clauseguard.core.domain.usecase.AnalyzeContractUseCase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.ktor.client.HttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Headless Execution Environment for ClauseGuard Core.
 * <p>
 * This Activity bypasses Jetpack Compose UI entirely and runs the full
 * contract analysis pipeline on application launch, printing results to
 * Logcat for validation without requiring an emulator UI interaction.
 * </p>
 *
 * Dependencies injected manually in onCreate:
 * - AndroidSqliteDriver for SQLDelight in-memory database
 * - Ktor HttpClient for FastAPI backend communication
 * - ContractRepositoryImpl bridging data and domain layers
 * - AnalyzeContractUseCase orchestrating the business workflow
 *
 * Pipeline execution runs on Dispatchers.IO and logs outcomes via Logcat.
 * Filter Logcat with "ClauseGuard-Core" to view output.
 */
@Deprecated("This activity is for headless validation only; remove or replace for production UI builds.")
class MainActivity : ComponentActivity() {

    private lateinit var httpClient: HttpClient
    private lateinit var repository: ContractRepositoryImpl
    private lateinit var analyzeUseCase: AnalyzeContractUseCase
    private lateinit var database: ClauseGuardDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**************************************************************************
         * Manual Dependency Graph — NO Compose UI initialization needed.
         * **************************************************************************/

        // 1. SQLDelight AndroidSqliteDriver
        val driver = AndroidSqliteDriver(ClauseGuardDatabase.Schema, this, "test.db")
        database = ClauseGuardDatabase(driver)

        // 2. Ktor HttpClient
        httpClient = KtorClient.getClient(this)

        // 3. ContractRepositoryImpl bridges data (SQLDelight) and domain layers
        repository = ContractRepositoryImpl(
            ktorClient = KtorClient,
            database = database
        )

        // 4. AnalyzeContractUseCase orchestrates: OCR → text → hash → LLM analysis → persist
        analyzeUseCase = AnalyzeContractUseCase(repository = repository)

        /**************************************************************************
         * Trigger the full pipeline on launch (Dispatchers.IO — no UI blocking).
         * **************************************************************************/
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Read dummy PDF from assets folder
                val inputStream = assets.open("dummy_contract.pdf")
                val dummyBytes = inputStream.readBytes()

                Log.d("ClauseGuard-Core", "Pipeline triggered with ${dummyBytes.size} bytes of dummy PDF data")

                // 5. Execute: analyzeContractUseCase(dummyBytes, "test_contract.pdf")
                val result = analyzeUseCase(dummyBytes, "test_contract.pdf")

                /**************************************************************************
                 * Fold: success vs failure logging to Logcat.
                 * **************************************************************************/
                result.fold(
                    onSuccess = { successResult ->
                        // Log the full contracted output to prove end-to-end functionality
                        val output = StringBuilder()
                        output.append("SUCCESS: Contract analyzed and persisted").append("\n")
                        output.append("  ID: ").append(successResult.id).append("\n")
                        output.append("  Document Hash: ").append(successResult.documentHash).append("\n")
                        output.append("  Filename: ").append(successResult.filename).append("\n")
                        output.append("  Risk Score: ").append(successResult.riskScore).append("\n")
                        output.append("  Clauses Count: ").append(successResult.clauses.size).append("\n")
                        for ((index, clause) in successResult.clauses.withIndex()) {
                            output.append("  Clause[$index]: ").append(clause.toString()).append("\n")
                        }
                        Log.d("ClauseGuard-Core", output.toString())
                    },
                    onFailure = { exception ->
                        // Log the failure with stack trace for debugging
                        Log.e("ClauseGuard-Core", "FAILED: ", exception)
                    }
                )
            } catch (e: Exception) {
                Log.e("ClauseGuard-Core", "Pipeline crashed with unexpected exception", e)
            }
        }
    }

    /**************************************************************************
     * Compose UI is intentionally omitted for this headless validation build.
     * The setContent block is provided as a no-op placeholder so the Activity
     * compiles and launches without a UI layer.
     * **************************************************************************/
    @Composable
    fun EmptyComposeUi(modifier: Modifier = Modifier) {
        // No-op: this Activity launches without rendering UI.
        // The pipeline runs via lifecycleScope.launch in onCreate above.
        setContent {
            MaterialTheme {
                Surface(
                    modifier = modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Deliberately empty — pipeline runs in lifecycleScope.launch above.
                }
            }
        }
    }
}