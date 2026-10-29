package app.hisaab.core.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import app.hisaab.R

/**
 * Bundled fonts in app/src/main/res/font/ (verified TrueType, Oct 2026):
 *   instrument_serif_regular.ttf, instrument_serif_italic.ttf (Google Fonts)
 *   geist_regular.ttf, geist_medium.ttf, geist_semibold.ttf (Geist project)
 * The rupee sign is always drawn in Geist (Sans) — Instrument Serif has no ₹ glyph (spec 9.3).
 */
val Serif: FontFamily = FontFamily(
    Font(R.font.instrument_serif_regular, FontWeight.Normal),
    Font(R.font.instrument_serif_italic, FontWeight.Normal, FontStyle.Italic)
)
val Sans: FontFamily = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold)
)

object HType {
    val HeroAmount = TextStyle(fontFamily = Serif, fontSize = 76.sp, lineHeight = 72.sp, letterSpacing = (-0.03).em)
    val DisplayAmount = TextStyle(fontFamily = Serif, fontSize = 44.sp, lineHeight = 44.sp, letterSpacing = (-0.02).em)
    val Headline = TextStyle(fontFamily = Serif, fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.01).em)
    val HeadlineItalic = Headline.copy(fontStyle = FontStyle.Italic)
    val Title = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.01).em)
    val Body = TextStyle(fontFamily = Sans, fontSize = 15.sp, lineHeight = 22.sp)
    val Label = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.01.em)
    val Caption = TextStyle(fontFamily = Sans, fontSize = 12.sp, lineHeight = 16.sp)
    val NumList = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, fontFeatureSettings = "tnum")
    val Button = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.sp)
}
