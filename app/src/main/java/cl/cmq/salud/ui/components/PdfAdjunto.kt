package cl.cmq.salud.ui.components

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal data class PaginaPdf(val imagen: Bitmap, val cantidad: Int)
private data class PdfUiState(val pagina: PaginaPdf? = null, val error: String? = null)

@Composable
fun PdfAdjunto(ruta: String, modifier: Modifier = Modifier) {
    var numero by remember(ruta) { mutableIntStateOf(0) }
    val estado by produceState(PdfUiState(), ruta, numero) {
        value = PdfUiState()
        value = withContext(Dispatchers.IO) {
            runCatching { PdfUiState(pagina = renderizarPaginaPdf(ruta, numero)) }.getOrElse {
                PdfUiState(error = "No se pudo visualizar este PDF. Puede estar dañado o protegido con contraseña.")
            }
        }
    }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
            val pagina = estado.pagina
            when {
                estado.error != null -> Text(requireNotNull(estado.error))
                pagina != null -> Image(pagina.imagen.asImageBitmap(), "Página ${numero + 1} del documento PDF",
                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                else -> CircularProgressIndicator()
            }
        }
        estado.pagina?.let { pagina ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (pagina.cantidad > 1) TextButton(onClick = { numero-- }, enabled = numero > 0) { Text("Anterior") }
                Text("${numero + 1} / ${pagina.cantidad}", style = MaterialTheme.typography.bodySmall)
                if (pagina.cantidad > 1) TextButton(onClick = { numero++ }, enabled = numero < pagina.cantidad - 1) { Text("Siguiente") }
            }
        }
    }
}

/** Solo una página acotada en memoria; se cierran página, renderer y descriptor en cada lectura. */
internal fun renderizarPaginaPdf(ruta: String, numero: Int = 0): PaginaPdf {
    ParcelFileDescriptor.open(File(ruta), ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
        PdfRenderer(descriptor).use { renderer ->
            renderer.openPage(numero).use { pagina ->
                val escala = minOf(1200f / pagina.width, 1600f / pagina.height)
                val ancho = (pagina.width * escala).toInt().coerceAtLeast(1)
                val alto = (pagina.height * escala).toInt().coerceAtLeast(1)
                val bitmap = Bitmap.createBitmap(ancho, alto, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.WHITE)
                    pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    return PaginaPdf(bitmap, renderer.pageCount)
                } catch (error: Exception) { bitmap.recycle(); throw error }
            }
        }
    }
}
