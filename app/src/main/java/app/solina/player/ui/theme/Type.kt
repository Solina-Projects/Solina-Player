package app.solina.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextGeometricTransform
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.solina.player.R
import app.solina.player.data.preferences.AppearanceFontFamilies


// Vazirmatn (Persian/Arabic UI face, SIL OFL 1.1, v33.003 — see
// app/src/main/assets/licenses/vazirmatn-OFL.txt). Variable font,
// weight axis 100-900. Used as the Persian glyph fallback inside
// RoundedSans / MontserratFamily below, so Persian text renders in
// Vazirmatn automatically wherever those families are used, with no
// call-site changes. Also usable directly as VazirmatnFamily.
@OptIn(ExperimentalTextApi::class)
val VazirmatnFamily = FontFamily(
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Light.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Normal.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Medium.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.SemiBold.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Bold.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.ExtraBold.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Black.weight))
    ),
)

@OptIn(ExperimentalTextApi::class)
val MontserratFamily = FontFamily(
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Black.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.ExtraBold.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Bold.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.SemiBold.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Medium.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Normal.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Light.weight))
    ),
    // Persian glyph fallback: GFlex has no Persian glyphs, so Persian
    // text falls through to Vazirmatn at the same weight (see note on
    // VazirmatnFamily above).
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Light.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Normal.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Medium.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.SemiBold.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Bold.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.ExtraBold.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Black.weight))
    ),
)

val ExpTitleTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 60.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.5f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    displayMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 50.sp,
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    ),
    titleMedium = TextStyle(
        fontFamily = MontserratFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        textGeometricTransform = TextGeometricTransform(scaleX = 1.3f),
        letterSpacing = (-0.02).em,
        lineHeight = 0.95.em,
        platformStyle = PlatformTextStyle(includeFontPadding = false)
    )
)

private const val RoundedSansFlexRond = 100f

@OptIn(ExperimentalTextApi::class)
val RoundedSans = FontFamily(
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Light.weight),
            FontVariation.Setting("ROND", RoundedSansFlexRond)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Normal.weight),
            FontVariation.Setting("ROND", RoundedSansFlexRond)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Medium.weight),
            FontVariation.Setting("ROND", RoundedSansFlexRond)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.SemiBold.weight),
            FontVariation.Setting("ROND", RoundedSansFlexRond)
        )
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.gflex_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(FontWeight.Bold.weight),
            FontVariation.Setting("ROND", RoundedSansFlexRond)
        )
    ),
    // Persian glyph fallback: GFlex has no Persian glyphs, so Persian
    // text falls through to Vazirmatn at the same weight (see note on
    // VazirmatnFamily above). This covers every direct RoundedSans use
    // in the app (200+ call sites) with no further changes.
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Light.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Normal.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Medium.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.SemiBold.weight))
    ),
    androidx.compose.ui.text.font.Font(
        resId = R.font.vazirmatn_variable,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(FontWeight.Bold.weight))
    ),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp,
        lineHeight = 56.sp,
        letterSpacing = 0.sp
    ),
    displayMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = RoundedSans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

/**
 * App typography in a chosen font family (appearance customisation,
 * bug-fix round 2026-11-17, item 6). Every style keeps its weight,
 * size and spacing — only the family changes, so switching between
 * Rounded (default), Vazirmatn and the system face is safe anywhere.
 */
fun typographyFor(fontFamily: FontFamily): Typography = Typography.copy(
    displayLarge = Typography.displayLarge.copy(fontFamily = fontFamily),
    displayMedium = Typography.displayMedium.copy(fontFamily = fontFamily),
    displaySmall = Typography.displaySmall.copy(fontFamily = fontFamily),
    headlineLarge = Typography.headlineLarge.copy(fontFamily = fontFamily),
    headlineMedium = Typography.headlineMedium.copy(fontFamily = fontFamily),
    headlineSmall = Typography.headlineSmall.copy(fontFamily = fontFamily),
    titleLarge = Typography.titleLarge.copy(fontFamily = fontFamily),
    titleMedium = Typography.titleMedium.copy(fontFamily = fontFamily),
    titleSmall = Typography.titleSmall.copy(fontFamily = fontFamily),
    bodyLarge = Typography.bodyLarge.copy(fontFamily = fontFamily),
    bodyMedium = Typography.bodyMedium.copy(fontFamily = fontFamily),
    bodySmall = Typography.bodySmall.copy(fontFamily = fontFamily),
    labelLarge = Typography.labelLarge.copy(fontFamily = fontFamily),
    labelMedium = Typography.labelMedium.copy(fontFamily = fontFamily),
    labelSmall = Typography.labelSmall.copy(fontFamily = fontFamily)
)

/** The [FontFamily] for an appearance font-family name. */
fun appearanceFontFamily(name: String): FontFamily = when (name) {
    AppearanceFontFamilies.VAZIRMATN -> VazirmatnFamily
    AppearanceFontFamilies.SYSTEM -> FontFamily.Default
    else -> RoundedSans
}
