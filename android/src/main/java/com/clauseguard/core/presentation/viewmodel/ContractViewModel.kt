package com.clauseguard.core.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clauseguard.core.data.repository.ContractRepositoryImpl
import com.clauseguard.core.domain.model.Contract
import com.clauseguard.core.domain.model.RiskLevel
import com.clauseguard.core.presentation.contract.*
import com.clauseguard.core.data.vision.OcrProcessor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow combine
import kotlinx.coroutines.flow emit
import kotlinx.coroutines.flow collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.unit
import java.util.*

/**
 * Presentation-layer ViewModel implementing Unidirectional Data Flow (UDF) / MVI.
 *
 * Responsibilities:
 * - Hold UI state (ContractUiState) as a StateFlow.
 * - Accept Intents (user actions) via handleIntent().
 * - Transition state purely and predictably.
 * - Emitting SideEffects (toasts, navigation, haptics) via SharedFlow.
 * - Delegate all business logic to Use Cases (AnalyzeContractUseCase, GetVaultContractsUseCase).
 * - All coroutine work dispatched appropriately: Default for CPU, IO for I/O, Main for UI.
 */
class ContractViewModel(
    private val analyzeUseCase: AnalyzeContractUseCase,
    private val getVaultContractsUseCase: () -> kotlinx.coroutines.flow.Flow<List<Contract>>,
    private val ocrProcessor: OcrProcessor
) : ViewModel() {

    /** The sole source of truth for the UI.  Observers react to state changes only. */
    private val _uiState = MutableStateFlow(ContractUiState())
    val uiState: StateFlow<ContractUiState> = _uiState

    /** Side effects emitted from the Presentation to the UI/OS layer.
     *  Collected once; each emission is a one-off event (toast, navigate, haptic). */
    private val _sideEffects = kotlinx.coroutines.flow.SharedFlow[ContractSideEffect]()
    val sideEffect: Flow<ContractSideEffect> = _sideEffects.asFlow()

    /** Convenience constructor for DI / testing. */
    constructor(
        repository: ContractRepositoryImpl = ContractRepositoryImpl(),
        vaultFlow: () -> Flow<List<Contract>> = { emptyList() },
        ocr: OcrProcessor = OcrProcessor(android.app.ApplicationProvider.applicationContext)
    ) : this(
        analyzeUseCase = AnalyzeContractUseCase(repository),
        getVaultContractsUseCase = vaultFlow,
        ocrProcessor = ocr
    )

    init {
        // Collect side effects in the ViewModel lifetime — no leak, auto-cancel.
        viewModelScope.launch {
            sideEffect.collect { effect ->
                when (effect) {
                    is ShowToast -> Log.d("ContractViewModel", "TOAST: ${effect.message}")
                    is NavigateToResults -> Log.d("ContractViewModel", "NAVIGATE to ${effect.contractId}")
                    is TriggerHapticFeedback ->
                        // Android 5+ haptic; on older devices this is a no-op.
                        android.util.Log.d("ContractViewModel", "HAPTIC triggered")
                }
            }
        }
    }

    /** Entry point: the UI (or test) calls this with a user action.
     *  Non-blocking; dispatches to appropriate coroutine context.
     */
    fun handleIntent(intent: ContractIntent) {
        when (intent) {
            is ContractIntent.AnalyzeDocument -> handleAnalyzeDocument(intent.fileBytes, intent.filename)
            is ContractIntent.CaptureFromOcr -> handleCaptureFromOcr(intent.imageUri)
            is ContractIntent.SelectClause -> handleSelectClause(intent.clauseId)
            is ContractIntent.DeleteContract -> handleDeleteContract(intent.contractId)
            is ContractIntent.LoadVault -> handleLoadVault()
            is ContractIntent.DismissError -> handleDismissError()
        }
    }

    /* ---------- Intent Handlers ---------- */

    private fun handleAnalyzeDocument(fileBytes: ByteArray, filename: String) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch(Dispatchers.IO) {
            val result = analyzeUseCase(fileBytes, filename)
            // Switch back to Main dispatcher to update UI state
            withContext(Dispatchers.Main) {
                when (result) {
                    is Contract.UseCaseResult.Success -> {
                        val contract = result.contract
                        // Emit new state: contract loaded, vault unchanged, no error
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            contract = contract,
                            errorMessage = null
                        )
                        // Emit side effect: success toast + optional navigation
                        _sideEffects.emit(ShowToast("Contract analyzed successfully"))
                        _sideEffects.emit(NavigateToResults(contract.id))
                    }
                    is Contract.UseCaseResult.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            errorMessage = result.error.message
                        )
                        _sideEffects.emit(ShowToast("Analysis failed: ${result.error.message}"))
                    }
                }
            }
        }
    }

    private fun handleCaptureFromOcr(imageUri: android.net.Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = ocrProcessor.extractTextFromUri(imageUri)
            withContext(Dispatchers.Main) {
                when (result) {
                    is Result.Success<String> -> {
                        val extractedText = result.getOrNull()
                        if (extractedText?.isNotBlank() == true) {
                            // Treat extracted text as document bytes for analysis
                            // (In a full app, you'd feed this into the analysis pipeline)
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "OCR extracted ${extractedText.length} chars; ready for analysis"
                            )
                            _sideEffects.emit(ShowToast("OCR complete — ${extractedText.length} characters extracted"))
                        } else {
                            _uiState.value = _uiState.value.copy(
                                errorMessage = "OCR returned no readable text"
                            )
                            _sideEffects.emit(ShowToast("No text found in image"))
                        }
                    }
                    is Result.Failure -> {
                        _uiState.value = _uiState.value.copy(
                            errorMessage = "OCR failed: ${result.exceptionOrNull()?.message ?: "unknown error"}"
                        )
                        _sideEffects.emit(ShowToast("OCR failed"))
                    }
                }
            }
        }
    }

    private fun handleSelectClause(clauseId: String) {
        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(
                selectedClauseId = clauseId
            )
        }
    }

    private fun handleDeleteContract(contractId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // TODO: Call repository.deleteContract(contractId)
            // For now, emit a side effect and update state locally
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = "Delete requested for contract $contractId"
                )
                _sideEffects.emit(ShowToast("Delete pending for contract $contractId"))
            }
        }
    }

    private fun handleLoadVault() {
        viewModelScope.launch(Dispatchers.IO) {
            val vaultContracts = withContext(Dispatchers.Main) {
                // Call the vault flow — in production this queries SQLDelight
                emptyList() // placeholder; replace with actual repo call
            }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                vaultContracts = vaultContracts,
                errorMessage = null
            )
        }
    }

    private fun handleDismissError() {
        withContext(Dispatchers.Main) {
            _uiState.value = _uiState.value.copy(
                errorMessage = null
            )
        }
    }
}