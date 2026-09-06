package com.clauseguard.core.presentation.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clauseguard.core.domain.model.Clause
import com.clauseguard.core.presentation.ui.theme.*

@Composable
fun ClauseCard(
    clause: Clause,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press feedback: scale down to 0.97f when pressed
    val scale = if (isPressed) 0.97f else 1f

    // Spring animation configuration matching the Apple-style critically damped spring from the prototype
    val expandSpring = spring<Int>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow // roughly equates to duration 0.35s
    )

    Column(
        modifier = modifier
            .scale(scale)
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(12.dp),
                ambientColor = Color.Black.copy(alpha = 0.3f),
                spotColor = Color.Black.copy(alpha = 0.3f)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(Surface1)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Custom scale indication handles feedback
                onClick = onToggle
            )
    ) {
        // --- Header Section (Always Visible) ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
                // Clause Number and Category
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(Surface2, RoundedCornerShape(4.dp))
                            .border(1.dp, Hairline, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "§ ${clause.id.take(2)}", // Using partial ID as clause number placeholder
                            color = TextSecondary,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = clause.category,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 18.sp
                    )
                }

                // Snippet (crossfades out when expanding)
                AnimatedVisibility(
                    visible = !isExpanded,
                    enter = fadeIn(animationSpec = expandSpring) + expandVertically(animationSpec = expandSpring),
                    exit = fadeOut(animationSpec = expandSpring) + shrinkVertically(animationSpec = expandSpring)
                ) {
                    Text(
                        text = clause.originalText,
                        color = TextSecondary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            RiskBadge(riskLevel = clause.riskLevel)
        }

        // --- Expanded Body Section ---
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = expandSpring) + expandVertically(animationSpec = expandSpring),
            exit = fadeOut(animationSpec = expandSpring) + shrinkVertically(animationSpec = expandSpring)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Surface1, Surface2.copy(alpha = 0.3f))
                        )
                    )
                    .border(width = 1.dp, color = Hairline) // Top border simulator
                    .padding(horizontal = 16.dp, vertical = 20.dp)
            ) {
                // Original Text
                Text(
                    text = "ORIGINAL TEXT",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Surface2.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                        .border(1.dp, Hairline, RoundedCornerShape(6.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = clause.originalText,
                        color = TextSecondary,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        lineHeight = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Plain English
                Text(
                    text = "PLAIN ENGLISH",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Text(
                    text = clause.explanation,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Strategy
                Text(
                    text = "STRATEGY",
                    color = AccentPurple,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentPurple.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                        .border(1.dp, AccentPurple.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                        .padding(14.dp)
                ) {
                    Text(
                        text = clause.negotiationStrategy,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }
    }
}