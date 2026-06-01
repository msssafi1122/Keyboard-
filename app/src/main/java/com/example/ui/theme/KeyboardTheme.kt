package com.example.ui.theme

import androidx.compose.ui.graphics.Color

data class KeyboardThemeColors(
    val background: Color,
    val keyBackground: Color,
    val keyLabel: Color,
    val specialKeyBackground: Color,
    val specialKeyLabel: Color,
    val accentColor: Color,
    val textSelectionColor: Color
)

object KeyboardThemes {
    val themes = mapOf(
        "Cosmic Dark" to KeyboardThemeColors(
            background = Color(0xFF0F0E17),
            keyBackground = Color(0xFF1F1E26),
            keyLabel = Color(0xFFFFFFFE),
            specialKeyBackground = Color(0xFFE53170),
            specialKeyLabel = Color(0xFFFFFFFF),
            accentColor = Color(0xFFFFD803),
            textSelectionColor = Color(0xFFE53170).copy(alpha = 0.3f)
        ),
        "Classic Light" to KeyboardThemeColors(
            background = Color(0xFFEAEBED),
            keyBackground = Color(0xFFFFFFFF),
            keyLabel = Color(0xFF1C1B1F),
            specialKeyBackground = Color(0xFFCFD2D8),
            specialKeyLabel = Color(0xFF2C2F36),
            accentColor = Color(0xFF1570EF),
            textSelectionColor = Color(0xFF1570EF).copy(alpha = 0.3f)
        ),
        "Sunset Orange" to KeyboardThemeColors(
            background = Color(0xFF2D1610),
            keyBackground = Color(0xFF4F2418),
            keyLabel = Color(0xFFFFECE5),
            specialKeyBackground = Color(0xFFE65100),
            specialKeyLabel = Color(0xFFFFFFFF),
            accentColor = Color(0xFFFF8F00),
            textSelectionColor = Color(0xFFE65100).copy(alpha = 0.3f)
        ),
        "Mint Fresh" to KeyboardThemeColors(
            background = Color(0xFFE4F0EC),
            keyBackground = Color(0xFFFFFFFF),
            keyLabel = Color(0xFF0F3A2E),
            specialKeyBackground = Color(0xFFB1DFD0),
            specialKeyLabel = Color(0xFF06221A),
            accentColor = Color(0xFF12B76A),
            textSelectionColor = Color(0xFF12B76A).copy(alpha = 0.3f)
        ),
        "Lavender Soft" to KeyboardThemeColors(
            background = Color(0xFF151221),
            keyBackground = Color(0xFF2C2442),
            keyLabel = Color(0xFFECE6FF),
            specialKeyBackground = Color(0xFF7433FF),
            specialKeyLabel = Color(0xFFFFFFFF),
            accentColor = Color(0xFFB392FF),
            textSelectionColor = Color(0xFF7433FF).copy(alpha = 0.3f)
        )
    )

    fun getColors(name: String): KeyboardThemeColors {
        return themes[name] ?: themes["Cosmic Dark"]!!
    }
}
