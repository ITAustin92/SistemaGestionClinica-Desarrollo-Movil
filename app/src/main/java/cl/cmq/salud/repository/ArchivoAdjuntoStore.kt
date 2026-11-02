package cl.cmq.salud.repository

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import cl.cmq.salud.model.Adjunto
import cl.cmq.salud.model.Documento
import cl.cmq.salud.repository.local.DocumentoEjemploPdf
import java.io.File
import java.io.IOException
import java.util.Locale
import java.util.UUID

/** Almacenamiento privado; no requiere permiso para leer o escribir todo el dispositivo. */
class ArchivoAdjuntoStore(private val context: Context) {
    private val pendientes get() = File(context.cacheDir, "adjuntos_pendientes").apply { mkdirs() }
    private val guardados get() = File(context.filesDir, "adjuntos").apply { mkdirs() }

    fun crearCaptura(): File = File.createTempFile("captura_", ".jpg", pendientes)

    fun crearEjemplo(documento: Documento): Adjunto {
        val archivo = File.createTempFile("ejemplo_", ".pdf", pendientes)
        try {
            DocumentoEjemploPdf.escribir(documento, archivo)
            return describir(archivo, "${documento.tipo.name.lowercase(Locale.ROOT)}_de_ejemplo.pdf")
        } catch (error: Exception) { archivo.delete(); throw error }
    }

    fun captura(ruta: String): Adjunto {
        val archivo = File(ruta)
        require(esDe(archivo, pendientes)) { "La captura no pertenece a esta aplicación." }
        return describir(archivo, "Documento escaneado.jpg").also {
            require(it.mimeType == "image/jpeg") { "La cámara no entregó una imagen JPEG válida." }
        }
    }

    fun importar(uri: Uri): Adjunto {
        val archivo = File(pendientes, "${UUID.randomUUID()}.adjunto")
        try {
            val entrada = requireNotNull(context.contentResolver.openInputStream(uri)) { "No se pudo abrir el archivo." }
            entrada.use { input ->
                archivo.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    var total = 0L
                    while (true) {
                        val cantidad = input.read(buffer)
                        if (cantidad < 0) break
                        total += cantidad
                        require(total <= MAX_BYTES) { "El archivo supera el límite de 10 MiB." }
                        output.write(buffer, 0, cantidad)
                    }
                }
            }
            return describir(archivo, "Documento importado")
        } catch (error: Exception) {
            archivo.delete()
            throw error
        }
    }

    private fun describir(archivo: File, nombre: String): Adjunto {
        require(archivo.isFile && archivo.length() in 1..MAX_BYTES) { "El archivo está vacío o supera los 10 MiB." }
        val cabecera = ByteArray(8)
        archivo.inputStream().use { it.read(cabecera) }
        val mime = when {
            cabecera.take(5).toByteArray().contentEquals("%PDF-".toByteArray(Charsets.US_ASCII)) -> "application/pdf"
            cabecera[0] == 0xFF.toByte() && cabecera[1] == 0xD8.toByte() -> "image/jpeg"
            cabecera.contentEquals(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) -> "image/png"
            else -> error("Selecciona un PDF, JPG o PNG válido.")
        }
        if (mime.startsWith("image/")) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(archivo.path, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "La imagen no se puede leer." }
        }
        val formato = when (mime) { "image/jpeg" -> "JPG"; "image/png" -> "PNG"; else -> "PDF" }
        val nombreFinal = if (nombre.contains('.')) nombre else "$nombre.${formato.lowercase(Locale.ROOT)}"
        return Adjunto(nombreFinal, formato, "${(archivo.length() + 1023) / 1024} KiB", archivo.absolutePath, mime)
    }

    fun conservar(adjunto: Adjunto): Adjunto {
        val origen = adjunto.rutaLocal?.let(::File) ?: return adjunto
        require(esDe(origen, pendientes)) { "El adjunto temporal no es válido. Vuelve a seleccionarlo." }
        // Revalidar justo antes de guardar; la imagen puede haber sido eliminada por el sistema.
        describir(origen, adjunto.nombre)
        val destino = File(guardados, "${UUID.randomUUID()}.${adjunto.formato.lowercase(Locale.ROOT)}")
        try { origen.copyTo(destino) } catch (error: Exception) { destino.delete(); throw error }
        return adjunto.copy(rutaLocal = destino.absolutePath)
    }

    fun descartar(ruta: String?) {
        ruta?.let(::File)?.takeIf { esDe(it, pendientes) }?.delete()
    }

    fun eliminarGuardado(ruta: String?) {
        ruta?.let(::File)?.takeIf { esDe(it, guardados) }?.delete()
    }

    fun limpiar(conservar: Set<String> = emptySet()) {
        // Solo hijos de los dos directorios privados gestionados por este almacén.
        listOf(pendientes, guardados).forEach { directorio ->
            directorio.listFiles()?.forEach { archivo ->
                if (archivo.absolutePath !in conservar && esDe(archivo, directorio) && archivo.isFile && !archivo.delete()) {
                    throw IOException("Se restauraron los datos, pero no se pudo eliminar un adjunto local.")
                }
            }
        }
    }

    fun archivoDisponible(adjunto: Adjunto): File {
        val archivo = requireNotNull(adjunto.rutaLocal?.let(::File)) { "Este registro es un ejemplo sin archivo real." }
        require((esDe(archivo, guardados) || esDe(archivo, pendientes)) && archivo.isFile) { "El adjunto ya no está disponible." }
        return archivo
    }

    private fun esDe(archivo: File, directorio: File) = archivo.canonicalFile.parentFile == directorio.canonicalFile

    companion object { const val MAX_BYTES = 10L * 1024 * 1024 }
}
