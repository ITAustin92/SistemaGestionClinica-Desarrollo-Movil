package cl.cmq.salud.ui.components

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import cl.cmq.salud.CmqApplication
import cl.cmq.salud.model.Adjunto
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class ImagenEstado(val bitmap: Bitmap? = null, val error: Boolean = false)

@Composable
fun ImagenAdjunto(adjunto: Adjunto, modifier: Modifier = Modifier) {
    val imagen by produceState(ImagenEstado(), adjunto.rutaLocal) {
        value = withContext(Dispatchers.IO) {
            runCatching { ImagenEstado(bitmap = decodificarImagenAdjunta(requireNotNull(adjunto.rutaLocal))) }
                .getOrElse { ImagenEstado(error = true) }
        }
    }
    Box(modifier, contentAlignment = Alignment.Center) {
        val bitmap = imagen.bitmap
        when {
            imagen.error -> Text("No se pudo cargar la imagen. Vuelve a capturar o seleccionar el archivo.")
            bitmap != null -> Image(bitmap.asImageBitmap(), "Vista previa del documento", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            else -> CircularProgressIndicator()
        }
    }
}

@Composable
fun VisorAdjunto(adjunto: Adjunto, onCerrar: () -> Unit, onError: (String) -> Unit) {
    val context = LocalContext.current
    var errorApertura by remember(adjunto.rutaLocal) { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.fillMaxWidth(0.94f).fillMaxHeight(0.9f).padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(adjunto.nombre, style = MaterialTheme.typography.titleLarge)
                Text("${adjunto.formato} · ${adjunto.tamano}")
                if (adjunto.mimeType.startsWith("image/")) {
                    ImagenAdjunto(adjunto, Modifier.fillMaxWidth().weight(1f))
                } else {
                    PdfAdjunto(requireNotNull(adjunto.rutaLocal), Modifier.fillMaxWidth().weight(1f))
                    OutlinedButton(onClick = {
                        try {
                            val store = (context.applicationContext as CmqApplication).archivos
                            val archivo = store.archivoDisponible(adjunto)
                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", archivo)
                            context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(uri, adjunto.mimeType)
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
                        } catch (_: android.content.ActivityNotFoundException) {
                            errorApertura = "No hay un visor PDF instalado en el dispositivo."
                            onError(requireNotNull(errorApertura))
                        } catch (error: Exception) {
                            errorApertura = error.message ?: "No se pudo abrir el adjunto."
                            onError(requireNotNull(errorApertura))
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Abrir con otra aplicación") }
                    errorApertura?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onCerrar, modifier = Modifier.align(Alignment.End)) { Text("Cerrar") }
            }
        }
    }
}

/** Decodifica una muestra acotada y respeta la orientación EXIF de la cámara. */
internal fun decodificarImagenAdjunta(ruta: String): Bitmap {
    val archivo = File(ruta)
    val limites = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(archivo.path, limites)
    require(limites.outWidth > 0 && limites.outHeight > 0) { "La imagen no se puede leer." }
    val opciones = BitmapFactory.Options().apply {
        inSampleSize = 1
        while (maxOf(limites.outWidth, limites.outHeight) / inSampleSize > 1600) inSampleSize *= 2
    }
    val bitmap = requireNotNull(BitmapFactory.decodeFile(archivo.path, opciones))
    val orientacion = runCatching { ExifInterface(archivo.path).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matriz = Matrix().apply {
        when (orientacion) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
        }
    }
    return if (matriz.isIdentity) bitmap else Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true).also {
        if (it !== bitmap) bitmap.recycle()
    }
}
