package com.clauseguard.core

import android.app.Activity
import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.disposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Size
import com.google.android.gms.vision.text.TextRecognizer
import com.zeroc.dev.h2.H2Driver
import com.zeroc.dev.h2.jdbc.JdbcSqliteDriver
import io.ktor.client.HttpClient
import io.ktor.client.request.DeleteRequest
import io.ktor.client.request.GetRequest
import io.ktor.client.request.HeadRequest
import io.ktor.client.request.OptionsRequest
import io.ktor.client.request.PatchRequest
import io.ktor.client.request.PutRequest
import io.ktor.client.request.Request
import io.ktor.client.request.RequestBuilder
import io.ktor.client.requestl
import io.ktor.client.routing.Get
import io.ktor.client.routing.Routing
import io.ktor.server.engine.Android
import io.ktor.server.internal.Application
import io.ktor.server.responders.Responder
import io.ktor.server.router.get
import io.ktor.server.router.Routing
import io.ktor.server.web.WebApplication
import javax.sql.DataSource

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /**************************************************************************
         * Manual Dependency Graph — NO Compose UI initialization needed.
         * **************************************************************************/

        // 1. SQLDelight AndroidSqliteDriver (in-memory for headless validation)
        val androidSqliteDriver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // 2. Ktor HttpClient — pointing to the local FastAPI backend.
        //    In debug/emulator mode, the base URL uses 10.0.2.2 which forwards
        //    to the host machine where the FastAPI server is running.
        httpClient = io.ktor.client.HttpClient(
            baseUrl = "http://10.0.2.2:8000/api/v1"
        ).apply {
            // Install default error handler so bad responses don't crash the app
            install(io.ktor.client.features.ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                })
            }
        }

        // 3. ContractRepositoryImpl bridges data (SQLDelight) and domain layers
        repository = ContractRepositoryImpl(
            ktorClient = httpClient,
            database = androidSqliteDriver
        )

        // 4. AnalyzeContractUseCase orchestrates: OCR → text → hash → LLM analysis → persist
        analyzeUseCase = AnalyzeContractUseCase(repository = repository)

        /**************************************************************************
         * Trigger the full pipeline on launch (Dispatchers.IO — no UI blocking).
         * **************************************************************************/
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Read dummy PDF from assets folder
                val assetFileDescriptor: AssetFileDescriptor = assets.openFd("dummy_contract.pdf")
                val inputStream = assets.openStream("dummy_contract.pdf")
                val dummyBytes = inputStream?.readBytes() ?: run {
                    Log.e("ClauseGuard-Core", "FAILED: Could not read dummy_contract.pdf from assets")
                    return@launch
                }

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