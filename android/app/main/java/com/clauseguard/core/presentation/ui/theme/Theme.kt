package com.clauseguard.core.presentation.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Linear-style Modern Tool / Builder SaaS Palette
val Ground = Color(0xFF08090A)
val Surface1 = Color(0xFF16171C)
val Surface2 = Color(0xFF1E1F25)
val Surface3 = Color(0xFF26272E)
val AccentPurple = Color(0xFF5E6AD2)
val TextPrimary = Color(0xFFF7F8F8)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF6B7280)
val Hairline = Color(0x0FFFFFFF) // 6% white

// Semantic Risk Colors
val RiskHigh = Color(0xFFEF4444)
val RiskMedium = Color(0xFFF59E0B)
val RiskLow = Color(0xFF10B981)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    background = Ground,
    surface = Surface1,
    surfaceVariant = Surface2,
    onPrimary = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = Hairline
)

@Composable
fun ClauseGuardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}