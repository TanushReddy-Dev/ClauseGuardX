package com.clauseguard.core.presentation.contract

import com.clauseguard.core.domain.model.Contract
import com.clauseguard.core.domain.model.RiskLevel

/**
 * MVI UI State for the Contract screen.
 * Holds the minimal data needed by the UI — completely immutable and
 * observable via StateFlow. No Android Context, no Lifecycle, no UI
 * toolkits. Pure data contract between Presentation and View layers.
 */
data class ContractUiState(
    val isLoading: Boolean = false,
    val contract: Contract? = null,
    val vaultContracts: List<Contract> = emptyList(),
    val errorMessage: String? = null,
    val selectedClauseId: String? = null
)

/** User actions — intents that enter the system via the Presentation layer. */
sealed interface ContractIntent {

    /** Upload a PDF file byte array and filename for AI analysis. */
    data class AnalyzeDocument(val fileBytes: ByteArray, val filename: String) : ContractIntent

    /** Pass an image URI to the headless OCR processor. */
    data class CaptureFromOcr(val imageUri: android.net.Uri) : ContractIntent

    /** Select a specific clause from the vaulted contract list. */
    data class SelectClause(val clauseId: String) : ContractIntent

    /** Permanently delete a contract from the vault. */
    data class DeleteContract(val contractId: String) : ContractIntent

    /** Load all contracts from the local vault (called on screen attach). */
    object LoadVault : ContractIntent

    /** Dismiss any displayed error message. */
    object DismissError : ContractIntent
}

/** One-off events emitted from the Presentation to the UI layer. */
sealed interface ContractSideEffect {

    /** Show a transient on-screen message (e.g., "Analysis complete"). */
    data class ShowToast(val message: String) : ContractSideEffect

    /** Navigate away from the current screen to a results/detail view. */
    data class NavigateToResults(val contractId: String) : ContractSideEffect

    /** Trigger device haptic feedback as a subtle success cue. */
    object TriggerHapticFeedback : ContractSideEffect
}