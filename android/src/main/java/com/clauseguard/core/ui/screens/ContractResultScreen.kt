package com.clauseguard.core.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.clauseguard.core.domain.model.Contract
import com.clauseguard.core.presentation.contract.ContractIntent
import com.clauseguard.core.presentation.contract.ContractSideEffect
import com.clauseguard.core.presentation.viewmodel.ContractViewModel
import com.clauseguard.core.ui.components.ClauseCard
import com.clauseguard.core.ui.theme.*
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractResultScreen(
    viewModel: ContractViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Handle One-Off Side Effects (Toasts, Haptics, Navigation)
    LaunchedEffect(viewModel.sideEffect) {
        viewModel.sideEffect.collectLatest { effect ->
            when (effect) {
                is ContractSideEffect.ShowToast -> {
                    // In a real app, wire to a SnackbarHostState.
                    // For now, handled by ViewModel log or trigger system UI.
                }
                is ContractSideEffect.TriggerHapticFeedback -> {
                    // Trigger device haptics
                }
                is ContractSideEffect.NavigateToResults -> { /* Handled elsewhere if needed */ }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Analysis Results",
                        style = Typography.titleMedium,
                        color = SlateText
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BackgroundLight,
                    titleContentColor = SlateText
                ),
                // Emulate a back button (would use real navigation icon in production)
                navigationIcon = {
                    TextButton(onClick = onNavigateBack) {
                        Text("Back", color = BrandBlue, style = Typography.bodyMedium)
                    }
                }
            )
        },
        containerColor = BackgroundLight
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState(modifier = Modifier.align(Alignment.Center))
                }
                uiState.errorMessage != null -> {
                    ErrorState(
                        message = uiState.errorMessage!!,
                        onRetry = { viewModel.handleIntent(ContractIntent.DismissError) },
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                uiState.contract != null -> {
                    // Peak Moment: Display the contract results
                    ContractContent(
                        contract = uiState.contract!!,
                        onClauseClick = { clauseId ->
                            viewModel.handleIntent(ContractIntent.SelectClause(clauseId))
                        }
                    )
                }
                else -> {
                    // Empty State
                    EmptyState(modifier = Modifier.align(Alignment.Center))
                }
            }
        }
    }
}

@Composable
private fun ContractContent(
    contract: Contract,
    onClauseClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp, start = 16.dp, end = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Peak Moment Header: Overall Score
        item {
            ContractScoreHeader(contract)
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Identified Clauses",
                style = Typography.titleMedium,
                color = SlateText
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // 2. Clause List
        items(contract.clauses, key = { it.id }) { clause ->
            ClauseCard(
                clause = clause,
                onClick = { onClauseClick(clause.id) }
            )
        }
    }
}

@Composable
private fun ContractScoreHeader(contract: Contract) {
    // 60/30/10 Rule applied. Focus on trust and clarity.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(SurfaceWhite)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = contract.filename,
            style = Typography.bodyMedium,
            color = SlateText.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Large authoritative score display
        Text(
            text = "${contract.riskScore ?: 0}/100",
            style = Typography.headlineLarge.copy(fontSize = 48.sp),
            color = SlateText
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Overall Risk Score",
            style = Typography.bodyLarge,
            color = SlateText.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun LoadingState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = BrandBlue)
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Analyzing contract terms...",
            style = Typography.bodyLarge,
            color = SlateText.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "This usually takes about 3 seconds.",
            style = Typography.bodyMedium,
            color = SlateText.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Red color reserved for meaningful error states
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(RiskHigh.copy(alpha = 0.1f), RoundedCornerShape(32.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("!", style = Typography.headlineLarge, color = RiskHigh)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Analysis Failed",
            style = Typography.titleMedium,
            color = SlateText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = Typography.bodyMedium,
            color = SlateText.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = BrandBlue),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Dismiss & Try Again", style = Typography.bodyLarge, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "No Contract Loaded",
            style = Typography.titleMedium,
            color = SlateText
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Upload a PDF or capture a document to begin analysis.",
            style = Typography.bodyMedium,
            color = SlateText.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )
    }
}