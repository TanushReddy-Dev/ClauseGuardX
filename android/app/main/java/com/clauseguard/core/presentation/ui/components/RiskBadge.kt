package com.clauseguard.core.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clauseguard.core.domain.model.RiskLevel
import com.clauseguard.core.presentation.ui.theme.RiskHigh
import com.clauseguard.core.presentation.ui.theme.RiskMedium
import com.clauseguard.core.presentation.ui.theme.RiskLow

@Composable
fun RiskBadge(riskLevel: RiskLevel, modifier: Modifier = Modifier) {
    val (color, bgColor) = when (riskLevel) {
        RiskLevel.HIGH -> RiskHigh to RiskHigh.copy(alpha = 0.1f)
        RiskLevel.MEDIUM -> RiskMedium to RiskMedium.copy(alpha = 0.1f)
        RiskLevel.LOW -> RiskLow to RiskLow.copy(alpha = 0.1f)
    }

    Box(
        modifier = modifier
            .background(color = bgColor, shape = RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Using standard Android material icons as placeholders
            // In a real app we'd use custom vector drawables for the Lucide icons
            Text(
                text = riskLevel.name,
                color = color,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            )
        }
    }
}