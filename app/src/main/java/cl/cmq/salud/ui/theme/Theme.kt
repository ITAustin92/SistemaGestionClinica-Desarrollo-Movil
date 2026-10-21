package cl.cmq.salud.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val Turquesa = Color(0xFF00858A)
val Aqua = Color(0xFFE5F5F5)
val Grafito = Color(0xFF242727)
val GrisTexto = Color(0xFF666D73)
val GrisFondo = Color(0xFFF4F5F5)
val Borde = Color(0xFFD6DADC)

private val Colores = lightColorScheme(
    primary = Turquesa, onPrimary = Color.White,
    primaryContainer = Aqua, onPrimaryContainer = Color(0xFF006A70),
    secondary = Turquesa, secondaryContainer = Aqua,
    background = Color.White, onBackground = Grafito,
    surface = Color.White, onSurface = Grafito,
    surfaceVariant = GrisFondo, onSurfaceVariant = GrisTexto,
    outline = Borde, outlineVariant = Color(0xFFE9EBEC),
    error = Color(0xFFB32630),
)
private val Tipografia = Typography(
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 27.sp, lineHeight = 33.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, lineHeight = 23.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
)

@Composable
fun CmqTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Colores, typography = Tipografia,
        shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(20.dp), large = RoundedCornerShape(24.dp)),
        content = content)
}
