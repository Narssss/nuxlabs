package com.israadev.nuxlauncher.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Bell Launcher Clean Slate & Cobalt Blue Palette
 */
object NuxColors {
    // Primary Cobalt & Royal Blue Palette
    val ForestGreen = Color(0xFF2563EB) // Royal Cobalt Blue Primary (#2563EB)
    val MintGreen = Color(0xFF3B82F6)   // Bright Azure Blue Accent (#3B82F6)
    val SageGreen = Color(0xFF1D4ED8)   // Deep Cobalt Blue (#1D4ED8)
    val SoftLime = Color(0xFF172554)    // Deep Navy Tint Container
    val LightGreen = Color(0x262563EB)  // 15% Royal Blue Tint Container

    // Clean Slate Navy Neutrals (Solid Modern Surfaces)
    val Background = Color(0xFF0B1120)      // Deep Slate Navy Background
    val SurfaceWhite = Color(0xEB131C31)    // Solid Slate Card Surface
    val SurfaceElevated = Color(0xF51A253C) // Elevated Panel Surface
    val SurfaceInput = Color(0xFF0E1629)    // Recessed Navy Input Surface
    val DarkGray = Color(0xFFF1F5F9)        // Crisp Cool White Text
    val GrayNeutral = Color(0xFF94A3B8)     // Muted Slate Subtitles
    val LightGray = Color(0xFF1E293B)       // Clean Slate Dividers
    val CardBorder = Color(0xFF2B3B59)      // Crisp Blue-Slate Borders
    val ErrorRed = Color(0xFFEF4444)        // Clean Red Error
    val Amber = Color(0xFFF59E0B)           // Warm Amber Warning
    val TextPrimary = Color(0xFFFFFFFF)     // Pure White Text

    // Legacy aliases for backward compatibility
    val Mint = SoftLime
    val MintDark = MintGreen
    val ForestDark = DarkGray
    val ForestLight = GrayNeutral
    val ForestMuted = GrayNeutral
    val Cream = Background
    val Coral = ErrorRed
    val TextMuted = GrayNeutral
    val SuccessGreen = ForestGreen
    val SkyBlue = Color(0xFF38BDF8)
}

object NuxSizes {
    val BorderWidth = 1.5.dp
    val CornerRadius = 8.dp
    val CornerRadiusLarge = 10.dp
    val CornerRadiusSmall = 6.dp
    val ShadowOffset = 2.dp
    val BorderDefault = BorderStroke(BorderWidth, NuxColors.CardBorder)
    val ShapeDefault = RoundedCornerShape(CornerRadius)
    val ShapeLarge = RoundedCornerShape(CornerRadiusLarge)
    val ShapeSmall = RoundedCornerShape(CornerRadiusSmall)
}
