package com.clauseguard.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import com.clauseguard.core.domain.model.Clause
import com.clauseguard.core.domain.model.RiskLevel
import com.clauseguard.core.ui.theme.*

@Composable
fun ClauseCard(
    clause: Clause,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (riskColor, riskText) = when (clause.riskLevel) {
        RiskLevel.HIGH -> RiskHigh to "High Risk"
        RiskLevel.MEDIUM -> RiskMedium to "Medium Risk"
        RiskLevel.LOW -> RiskLow to "Low Risk"
    }

    // 8-point grid, soft shadow, rounded corners
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), ambientColor = SlateText.copy(alpha = 0.05f))
            .clip(RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp) // Generous breathing room
        ) {
            // Header: Category & Risk Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = clause.category,
                    style = MaterialTheme.typography.titleMedium,
                    color = SlateText
                )

                // Risk Badge (10% accent application)
                Box(
                    modifier = Modifier
                        .background(riskColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = riskText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = riskColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Explanation (F-Pattern scan focus)
            Text(
                text = clause.explanation,
                style = MaterialTheme.typography.bodyLarge,
                color = SlateText.copy(alpha = 0.8f) // 80% opacity for body
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Strategy Box (Subtle visual grouping)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BackgroundLight, RoundedCornerShape(8.dp))
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "Negotiation Strategy",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold),
                        color = BrandBlue
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = clause.negotiationStrategy,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SlateText.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}