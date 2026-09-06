package com.clauseguard.core.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// 60/30/10 Rule applied.

val BrandBlue = Color(0xFF2563EB) // 10% - Trust & primary CTAs
val SlateText = Color(0xFF0F172A) // 30% - High contrast readability
val SurfaceWhite = Color(0xFFFFFFFF) // 60% - Crisp cards
val BackgroundLight = Color(0xFFF8FAFC) // 60% - Reduces eye strain

// Semantic Colors (Risk Levels)
val RiskHigh = Color(0xFFEF4444) // Rose
val RiskMedium = Color(0xFFF59E0B) // Amber
val RiskLow = Color(0xFF10B981) // Emerald

val ClauseGuardColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    background = BackgroundLight,
    onBackground = SlateText,
    surface = SurfaceWhite,
    onSurface = SlateText,
    error = RiskHigh,
    onError = Color.White
)