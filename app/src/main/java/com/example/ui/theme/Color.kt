package com.example.ui.theme

import androidx.compose.ui.graphics.Color

val SamPurplePrimary = Color(0xFFA855F7)
val SamPurpleSecondary = Color(0xFFC084FC)
val SamCyan = Color(0xFF06B6D4)
val SamCyanGlow = Color(0xFF22D3EE)
val SamMagenta = Color(0xFFEC4899)
val SamPinkGlow = Color(0xFFF472B6)

val SamDarkBg = Color(0xFF0A0814)
val SamSurface = Color(0xFF131024)
val SamSurfaceElevated = Color(0xFF1E1A38)
val SamSurfaceBorder = Color(0xFF2E2754)

val SamTextPrimary = Color(0xFFF8FAFC)
val SamTextSecondary = Color(0xFF94A3B8)
val SamTextMuted = Color(0xFF64748B)

val SamListeningGlow = Color(0xFF06B6D4)
val SamSpeakingGlow = Color(0xFFA855F7)
val SamIdleGlow = Color(0xFF6366F1)
val SamErrorRed = Color(0xFFF43F5E)
val SamSuccessGreen = Color(0xFF10B981)

enum class AppThemeAccent(
    val displayName: String,
    val primary: Color,
    val secondary: Color,
    val tertiary: Color,
    val glow: Color
) {
    CLASSIC(
        displayName = "Classic Sam",
        primary = Color(0xFFA855F7),     // Purple
        secondary = Color(0xFFC084FC),   // Light Purple
        tertiary = Color(0xFFEC4899),    // Magenta
        glow = Color(0xFF22D3EE)         // Cyan
    ),
    NEON_BLUE(
        displayName = "Neon Blue",
        primary = Color(0xFF0284C7),     // Sky Blue
        secondary = Color(0xFF38BDF8),   // Light Sky Blue
        tertiary = Color(0xFF06B6D4),    // Cyan
        glow = Color(0xFF00F0FF)         // Neon Blue
    ),
    AMBER_GOLD(
        displayName = "Amber Gold",
        primary = Color(0xFFD97706),     // Amber
        secondary = Color(0xFFFBBF24),   // Yellow
        tertiary = Color(0xFFF97316),    // Orange
        glow = Color(0xFFFCD34D)         // Gold
    ),
    MATRIX_GREEN(
        displayName = "Matrix Green",
        primary = Color(0xFF16A34A),     // Green
        secondary = Color(0xFF4ADE80),   // Light Green
        tertiary = Color(0xFF0D9488),    // Teal
        glow = Color(0xFF22C55E)         // Lime Glow
    ),
    ROSE_QUARTZ(
        displayName = "Rose Quartz",
        primary = Color(0xFFDB2777),     // Rose
        secondary = Color(0xFFF472B6),   // Pink
        tertiary = Color(0xFFE11D48),    // Crimson
        glow = Color(0xFFFDA4AF)         // Rose Glow
    )
}
