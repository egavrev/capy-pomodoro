package com.capypomodoro.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.capypomodoro.R

/** Design tokens from HANDOFF.md. */
object CapyColors {
    val BlushBg = Color(0xFFFFF5F8)
    val Dot = Color(0xFFF7C9D6)
    val Surface = Color(0xFFFFFFFF)
    val PinkSoft = Color(0xFFFCE1E9)
    val PinkTint = Color(0xFFFFF0F4)
    val PinkLine = Color(0xFFFBE3EA)
    val PinkProgress = Color(0xFFE77C99)
    val Rose = Color(0xFFB8456A)
    val BowFill = Color(0xFFF7B3C4)
    val BowStroke = Color(0xFFC9506F)
    val BowKnot = Color(0xFFF29BB3)
    val Ink = Color(0xFF4A2C36)
    val Muted = Color(0xFF7E5A66)
    val ToggleOff = Color(0xFFE9D3DA)
    val TileBorder = Color(0xFFF3D3DD)
    val Cream = Color(0xFFFFF8F3)

    /** Rose-tinted shadow colours: never grey. */
    val ShadowAmbient = Rose.copy(alpha = 0.10f)
    val ShadowSpot = Rose.copy(alpha = 0.30f)
}

val Fredoka = FontFamily(
    Font(R.font.fredoka_semibold, FontWeight.SemiBold),
)

val Nunito = FontFamily(
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

/** Text styles used across the screens, named after their role in the designs. */
object CapyType {
    val AppName = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, color = CapyColors.Ink)
    val TimerFocus = TextStyle(
        fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 64.sp, lineHeight = 64.sp,
        letterSpacing = 1.sp, color = CapyColors.Ink, fontFeatureSettings = "tnum",
    )
    val TimerBreak = TimerFocus.copy(fontSize = 60.sp, lineHeight = 60.sp, letterSpacing = 0.sp)
    val ScreenTitle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 26.sp, color = CapyColors.Ink)
    val SectionTitle = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, color = CapyColors.Rose)
    val StepperValue = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = CapyColors.Ink)
    val Button = TextStyle(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)

    val PhaseLine = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = CapyColors.Rose)
    val Tab = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
    val RowLabel = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = CapyColors.Ink)
    val RowHint = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = CapyColors.Muted)
    val Caption = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = CapyColors.Muted)
    val CardTitle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = CapyColors.Ink)
    val CardSub = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = CapyColors.Muted)
    val Chip = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    val GroupTitle = TextStyle(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = CapyColors.Ink)
}

private val CapyColorScheme = lightColorScheme(
    primary = CapyColors.Rose,
    onPrimary = Color.White,
    primaryContainer = CapyColors.PinkSoft,
    onPrimaryContainer = CapyColors.Ink,
    secondary = CapyColors.PinkProgress,
    onSecondary = Color.White,
    secondaryContainer = CapyColors.PinkTint,
    onSecondaryContainer = CapyColors.Rose,
    background = CapyColors.BlushBg,
    onBackground = CapyColors.Ink,
    surface = CapyColors.Surface,
    onSurface = CapyColors.Ink,
    surfaceVariant = CapyColors.PinkTint,
    onSurfaceVariant = CapyColors.Muted,
    outline = CapyColors.ToggleOff,
    outlineVariant = CapyColors.PinkLine,
)

private val CapyTypography = Typography().run {
    copy(
        displayLarge = displayLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        displayMedium = displayMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        displaySmall = displaySmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        headlineLarge = headlineLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        headlineMedium = headlineMedium.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        headlineSmall = headlineSmall.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        titleLarge = titleLarge.copy(fontFamily = Fredoka, fontWeight = FontWeight.SemiBold),
        titleMedium = titleMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
        titleSmall = titleSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
        bodyLarge = bodyLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
        bodyMedium = bodyMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
        bodySmall = bodySmall.copy(fontFamily = Nunito, fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontFamily = Nunito, fontWeight = FontWeight.ExtraBold),
        labelMedium = labelMedium.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
        labelSmall = labelSmall.copy(fontFamily = Nunito, fontWeight = FontWeight.Bold),
    )
}

/** Pills are fully rounded; cards 22–30dp; image tiles 18dp. */
object CapyShapes {
    val Pill = RoundedCornerShape(percent = 50)
    val Card = RoundedCornerShape(22.dp)
    val SettingsCard = RoundedCornerShape(26.dp)
    val TimerCard = RoundedCornerShape(30.dp)
    val Tile = RoundedCornerShape(18.dp)
    val IconTile = RoundedCornerShape(14.dp)
    val Chip = RoundedCornerShape(16.dp)
}

private val CapyMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun CapyTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CapyColorScheme,
        typography = CapyTypography,
        shapes = CapyMaterialShapes,
        content = content,
    )
}
