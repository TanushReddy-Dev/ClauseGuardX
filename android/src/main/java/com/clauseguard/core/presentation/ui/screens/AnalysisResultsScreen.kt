package com.clauseguard.core.presentation.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clauseguard.core.presentation.contract.ContractIntent
import com.clauseguard.core.presentation.contract.ContractUiState
import com.clauseguard.core.presentation.ui.components.ClauseCard
import com.clauseguard.core.presentation.ui.theme.*

@Composable
fun AnalysisResultsScreen(
    uiState: ContractUiState,
    onIntent: (ContractIntent) -> Unit
) {
    val contract = uiState.contract

    if (uiState.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentPurple)
        }
        return
    }

    if (uiState.errorMessage != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Error: ${uiState.errorMessage}", color = RiskHigh)
        }
        return
    }

    if (contract == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No contract data available", color = TextSecondary)
        }
        return
    }

    // Determine score color based on thresholds
    val scoreColor = when {
        (contract.riskScore ?: 0) >= 70 -> RiskHigh
        (contract.riskScore ?: 0) >= 40 -> RiskMedium
        else -> RiskLow
    }

    // --- Peak Moment Entrance Animation ---
    // Animating the score scaling up on mount
    val scoreScale = remember { Animatable(0.9f) }
    val scoreAlpha = remember { Animatable(0f) }

    LaunchedEffect(contract.id) {
        scoreScale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    LaunchedEffect(contract.id) {
        scoreAlpha.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ground)
    ) {
        // --- Sticky Glass Header ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Ground.copy(alpha = 0.75f))
                // Note: True blur requires an accompanist library or Android 12+ RenderEffect.
                // For simplicity, we use semi-transparent background here.
                .padding(horizontal = 16.dp)
                .padding(top = 48.dp, bottom = 16.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Placeholder for back button icon
                Text("< Back", color = TextSecondary, fontSize = 14.sp)

                Text(
                    text = contract.filename,
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false).padding(horizontal = 16.dp)
                )

                // Placeholder for share button
                Text("Share", color = TextSecondary, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score Section
            Row(verticalAlignment = Alignment.Bottom) {
                Column {
                    Text(
                        text = "OVERALL RISK",
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = contract.riskScore?.toString() ?: "--",
                        color = scoreColor,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1.5).sp,
                        modifier = Modifier
                            .scale(scoreScale.value)
                            .alpha(scoreAlpha.value)
                    )
                }
                Text(
                    text = "/ 100",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                )
            }
        }

        // --- Scrollable Content ---
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Identified Clauses (${contract.clauses.size})",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "COLLAPSE ALL",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable {
                            onIntent(ContractIntent.SelectClause("")) // Clear selection
                        }
                    )
                }
            }

            itemsIndexed(contract.clauses) { index, clause ->
                // Delay entrance based on index for stagger effect
                val clauseAlpha = remember { Animatable(0f) }
                val clauseY = remember { Animatable(16f) }

                LaunchedEffect(clause.id) {
                    kotlinx.coroutines.delay(index * 60L) // 60ms stagger
                    clauseAlpha.animateTo(1f)
                }
                LaunchedEffect(clause.id) {
                    kotlinx.coroutines.delay(index * 60L)
                    clauseY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                }

                ClauseCard(
                    clause = clause,
                    isExpanded = uiState.selectedClauseId == clause.id,
                    onToggle = {
                        val newId = if (uiState.selectedClauseId == clause.id) "" else clause.id
                        onIntent(ContractIntent.SelectClause(newId))
                    },
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .alpha(clauseAlpha.value)
                        .offset(y = clauseY.value.dp)
                )
            }
        }
    }

    // Bottom Action Bar Simulator
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Gradient overlay
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Ground)
                    )
                )
        )

        // Export Button
        Box(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .background(AccentPurple, RoundedCornerShape(12.dp))
                .clickable { /* Handle export */ }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Export Summary",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}