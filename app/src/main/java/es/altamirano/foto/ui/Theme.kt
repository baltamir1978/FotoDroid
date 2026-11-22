package es.altamirano.foto.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import es.altamirano.common.ui.AppTheme
import es.altamirano.common.ui.withBrandSelection

/** Las fuentes del sistema del Pixel (Google Sans); si no están, Roboto. */
private fun device(name: String) = FontFamily(
    Font(DeviceFontFamilyName(name), FontWeight.Normal),
    Font(DeviceFontFamilyName(name), FontWeight.Medium),
    Font(DeviceFontFamilyName(name), FontWeight.SemiBold),
    Font(DeviceFontFamilyName(name), FontWeight.Bold),
)

private val Display = device("google-sans")
private val Text = device("google-sans-text")

private fun TextStyle.with(f: FontFamily, w: FontWeight? = null) = copy(fontFamily = f, fontWeight = w ?: fontWeight)

private val base = Typography()
private val typography = Typography(
    displayLarge = base.displayLarge.with(Display), displayMedium = base.displayMedium.with(Display),
    displaySmall = base.displaySmall.with(Display),
    headlineLarge = base.headlineLarge.with(Display, FontWeight.Medium), headlineMedium = base.headlineMedium.with(Display, FontWeight.Medium),
    headlineSmall = base.headlineSmall.with(Display, FontWeight.Medium),
    titleLarge = base.titleLarge.with(Display, FontWeight.Medium), titleMedium = base.titleMedium.with(Display, FontWeight.Medium),
    titleSmall = base.titleSmall.with(Text, FontWeight.Medium),
    bodyLarge = base.bodyLarge.with(Text), bodyMedium = base.bodyMedium.with(Text), bodySmall = base.bodySmall.with(Text),
    labelLarge = base.labelLarge.with(Text, FontWeight.Medium), labelMedium = base.labelMedium.with(Text, FontWeight.Medium),
    labelSmall = base.labelSmall.with(Text, FontWeight.Medium),
)

@Composable
fun FotoTheme(content: @Composable () -> Unit) {
    val ctx = LocalContext.current
    // Colores del sistema, pero lo seleccionado en el azul clarito del icono: con algunos fondos de
    // pantalla salía gris. Material 3 Expressive y sus esquinas, de droid-common (como todas).
    val scheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(ctx).withBrandSelection(Color(0xFF9CC7E8), dark = true)
        else dynamicLightColorScheme(ctx).withBrandSelection(Color(0xFF2B5876), dark = false)
    AppTheme(scheme, typography = typography, content = content)
}
