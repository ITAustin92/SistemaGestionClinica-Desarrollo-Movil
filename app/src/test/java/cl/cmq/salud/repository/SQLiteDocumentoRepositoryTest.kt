package cl.cmq.salud.repository

import android.app.Application
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import cl.cmq.salud.ui.components.decodificarImagenAdjunta
import cl.cmq.salud.model.*
import cl.cmq.salud.repository.local.DocumentoDatabase
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.RandomAccessFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class SQLiteDocumentoRepositoryTest {
    private lateinit var context: Application
    private lateinit var archivos: ArchivoAdjuntoStore
    private lateinit var repository: SQLiteDocumentoRepository
    private val nombre = "persistencia_test.db"

    @Before fun preparar() {
        context = RuntimeEnvironment.getApplication()
        context.deleteDatabase(nombre)
        archivos = ArchivoAdjuntoStore(context)
        archivos.limpiar()
        repository = SQLiteDocumentoRepository(context, archivos, nombre)
        repository.cargar()
    }

    @After fun terminar() {
        repository.close()
        context.deleteDatabase(nombre)
        archivos.limpiar()
    }

    private fun importarPDF(): Adjunto {
        val origen = File(context.cacheDir, "entrada.pdf").apply {
            writeText("%PDF-1.4\nDocumento de prueba de almacenamiento\n%%EOF")
        }
        return archivos.importar(Uri.fromFile(origen)).also { origen.delete() }
    }

    private fun reabrir() {
        repository.close()
        repository = SQLiteDocumentoRepository(context, archivos, nombre).apply { cargar() }
    }

    @Test fun documentoAdjuntoDecisionEHistorialSobrevivenAReabrirLaBase() {
        val temporal = importarPDF()
        val id = repository.registrar(repository.usuarios.first(), "Documento persistente", TipoDocumento.CONTRATO, "Original", temporal)
        val ruta = requireNotNull(repository.documentos.value.first { it.id == id }.adjunto.rutaLocal)
        assertTrue(File(ruta).exists())
        assertFalse(File(requireNotNull(temporal.rutaLocal)).exists())
        repository.revisar(repository.usuarios.last(), id, EstadoDocumento.RECHAZADO, "Falta la firma")
        val esperado = repository.documentos.value
        reabrir()
        assertEquals(esperado, repository.documentos.value)
        assertTrue(File(ruta).exists())
        assertEquals("Falta la firma", repository.buscar(repository.usuarios.first(), id)?.revision?.observacion)
        assertEquals(2, repository.buscar(repository.usuarios.first(), id)?.historial?.size)
        assertNull(repository.buscar(repository.usuarios[1], id))
    }

    @Test fun reinicioRestauraEjemplosYEliminaArchivosDeTodosLosPerfiles() {
        val primero = repository.registrar(repository.usuarios[0], "Documento A", TipoDocumento.CERTIFICADO, "", importarPDF())
        repository.registrar(repository.usuarios[1], "Documento B", TipoDocumento.CONTRATO, "", importarPDF())
        repository.revisar(repository.usuarios.last(), primero, EstadoDocumento.APROBADO, "Revisado")
        val temporal = importarPDF()
        // Un archivo ajeno al almacén debe sobrevivir al reinicio.
        val ajeno = File(context.filesDir, "no_es_adjunto.txt").apply { writeText("Conservar") }
        repository.reiniciar()
        val ejemplos = DemoDocumentoRepository().documentos.value.sortedByDescending { it.id }
        assertEquals(ejemplos, repository.documentos.value.map { it.copy(adjunto = ejemplos.first { ejemplo -> ejemplo.id == it.id }.adjunto) })
        assertEquals(5, File(context.filesDir, "adjuntos").listFiles().orEmpty().size)
        assertTrue(repository.documentos.value.all { archivos.archivoDisponible(it.adjunto).exists() })
        assertFalse(File(requireNotNull(temporal.rutaLocal)).exists())
        assertTrue(ajeno.exists())
        ajeno.delete()
        reabrir()
        assertEquals(5, repository.documentos.value.size)
    }

    @Test fun baseVaciaInicializadaNoVuelveAInsertarEjemplos() {
        repository.close()
        DocumentoDatabase(context, nombre).use { it.guardar(emptyList()) }
        repository = SQLiteDocumentoRepository(context, archivos, nombre).apply { cargar() }
        assertTrue(repository.documentos.value.isEmpty())
    }

    @Test fun migracionDeNombresYAdjuntosAntiguosConservaRegistrosYRevisiones() {
        val id = repository.registrar(repository.usuarios.first(), "Documento guardado", TipoDocumento.CONTRATO, "", importarPDF())
        repository.revisar(repository.usuarios.last(), id, EstadoDocumento.APROBADO, "Correcto")
        val esperado = repository.documentos.value
        val propios = esperado.first { it.id == id }
        val legado = esperado.map { doc ->
            doc.copy(titular = when (doc.titularId) { 1L -> "Funcionario Demo A"; 2L -> "Funcionario Demo B"; else -> "Funcionario Demo C" },
                revision = doc.revision?.copy(revisor = "Revisor Demo A"),
                historial = doc.historial.map { evento -> evento.copy(actor = if (evento.actor == repository.usuarios.last().nombre) "Revisor Demo A" else "Funcionario Demo A") },
                adjunto = if (doc.id == 1L) Adjunto("documento_demo.pdf", "PDF") else doc.adjunto)
        }
        DocumentoDatabase(context, nombre).use { it.guardar(legado) }
        reabrir()
        val recuperado = requireNotNull(repository.buscar(repository.usuarios.first(), id))
        assertEquals(propios, recuperado)
        assertEquals("Ángel Quintero", recuperado.titular)
        assertEquals("Dr. Pedro Soto", recuperado.revision?.revisor)
        val ejemploActualizado = repository.documentos.value.first { it.id == 1L }.adjunto
        assertTrue(archivos.archivoDisponible(ejemploActualizado).readText(Charsets.US_ASCII).startsWith("%PDF-1.4"))
        assertEquals(esperado.size, repository.documentos.value.size)
    }

    @Test fun ejemploRegistradoDesdeFormularioSeConvierteEnArchivoPdfPersistente() {
        val id = repository.registrar(repository.usuarios[1], "Constancia de Food Fontus", TipoDocumento.CERTIFICADO,
            "", Adjunto("documento_de_ejemplo.pdf", "PDF"))
        reabrir()
        val documento = requireNotNull(repository.buscar(repository.usuarios[1], id))
        val pdf = archivos.archivoDisponible(documento.adjunto).readText(Charsets.US_ASCII)
        assertTrue(pdf.startsWith("%PDF-1.4"))
        assertTrue(pdf.contains("Constancia de Food Fontus"))
        assertTrue(pdf.contains("Food Fontus"))
    }

    @Test fun centrosYDoctorSeActualizanSinPerderArchivosNiCambiarPermisos() {
        val id = repository.registrar(repository.usuarios.first(), "Archivo del usuario", TipoDocumento.CONTRATO, "", importarPDF())
        repository.revisar(repository.usuarios.last(), id, EstadoDocumento.APROBADO, "Correcto")
        val adjuntoUsuario = requireNotNull(repository.buscar(repository.usuarios.first(), id)).adjunto
        val pdfAnterior = requireNotNull(repository.documentos.value.first { it.id == 1L }.adjunto.rutaLocal)
        val legado = repository.documentos.value.map { doc -> doc.copy(
            centro = if (doc.titularId == 3L) "Centro Demo B" else "Centro Demo A",
            revision = doc.revision?.copy(revisor = "Dr. Nombre Genérico"),
            historial = doc.historial.map { evento -> evento.copy(
                actor = if (evento.actor == "Dr. Pedro Soto") "Dr. Nombre Genérico" else evento.actor) }) }
        DocumentoDatabase(context, nombre).use { it.guardar(legado) }
        reabrir()
        val revisor = repository.usuarios.last()
        assertEquals("Dr. Pedro Soto", revisor.nombre)
        assertEquals("Centro Médico Quilpué", revisor.centro)
        assertEquals("Centro Médico Belloto", repository.usuarios[2].centro)
        assertNull(repository.buscar(revisor, 5L))
        val recuperado = requireNotNull(repository.buscar(revisor, id))
        assertEquals("Centro Médico Quilpué", recuperado.centro)
        assertEquals("Dr. Pedro Soto", recuperado.revision?.revisor)
        assertEquals(adjuntoUsuario, recuperado.adjunto)
        assertTrue(archivos.archivoDisponible(adjuntoUsuario).exists())
        assertFalse(File(pdfAnterior).exists())
        assertNotEquals(pdfAnterior, repository.documentos.value.first { it.id == 1L }.adjunto.rutaLocal)
        val actualizados = repository.documentos.value
        reabrir()
        assertEquals(actualizados, repository.documentos.value)
    }

    @Test fun falloDeSQLiteNoPublicaRegistroNiDejaCopiaDefinitiva() {
        DocumentoDatabase(context, nombre).use {
            it.writableDatabase.execSQL("""CREATE TRIGGER impedir_alta BEFORE INSERT ON documentos
                WHEN NEW.titulo = 'Fallo controlado' BEGIN SELECT RAISE(ABORT, 'Fallo de prueba'); END""")
        }
        val anteriores = repository.documentos.value
        val archivosAnteriores = File(context.filesDir, "adjuntos").listFiles().orEmpty().map { it.name }.toSet()
        val adjunto = importarPDF()
        assertThrows(Exception::class.java) {
            repository.registrar(repository.usuarios.first(), "Fallo controlado", TipoDocumento.CONTRATO, "", adjunto)
        }
        assertEquals(anteriores, repository.documentos.value)
        assertEquals(archivosAnteriores, File(context.filesDir, "adjuntos").listFiles().orEmpty().map { it.name }.toSet())
        reabrir()
        assertEquals(anteriores, repository.documentos.value)
    }

    @Test fun capturaJPEGSeGuardaYPermaneceTrasReabrir() {
        val captura = archivos.crearCaptura()
        val bitmap = Bitmap.createBitmap(100, 150, Bitmap.Config.ARGB_8888)
        captura.outputStream().use { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
        bitmap.recycle()
        ExifInterface(captura.path).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }
        val adjunto = archivos.captura(captura.path)
        assertEquals("image/jpeg", adjunto.mimeType)
        val id = repository.registrar(repository.usuarios.first(), "Página escaneada", TipoDocumento.ANTECEDENTE, "", adjunto)
        reabrir()
        val guardado = requireNotNull(repository.buscar(repository.usuarios.first(), id)).adjunto
        assertEquals("JPG", guardado.formato)
        assertTrue(archivos.archivoDisponible(guardado).length() > 0)
        val vistaPrevia = decodificarImagenAdjunta(archivos.archivoDisponible(guardado).path)
        assertEquals(150, vistaPrevia.width)
        assertEquals(100, vistaPrevia.height)
        vistaPrevia.recycle()
    }

    @Test fun cancelarCapturaEliminaSoloTemporalYConservaAdjuntoAnterior() {
        val adjunto = importarPDF()
        val captura = archivos.crearCaptura()
        archivos.descartar(captura.path)
        assertFalse(captura.exists())
        assertTrue(File(requireNotNull(adjunto.rutaLocal)).exists())
    }

    @Test fun contratoDeCamaraYProveedorApuntanAlDirectorioDeCapturas() {
        val captura = archivos.crearCaptura()
        val autoridad = "${context.packageName}.archivos"
        val provider = requireNotNull(context.packageManager.resolveContentProvider(autoridad, PackageManager.GET_META_DATA))
        assertFalse(provider.exported)
        assertTrue(provider.grantUriPermissions)
        val xml = requireNotNull(provider.loadXmlMetaData(context.packageManager, "android.support.FILE_PROVIDER_PATHS"))
        val rutas = mutableMapOf<String, String>()
        xml.use {
            while (it.eventType != org.xmlpull.v1.XmlPullParser.END_DOCUMENT) {
                if (it.eventType == org.xmlpull.v1.XmlPullParser.START_TAG && it.name == "cache-path") {
                    rutas[it.getAttributeValue(null, "name")] = it.getAttributeValue(null, "path")
                }
                it.next()
            }
        }
        val raiz = File(context.cacheDir, requireNotNull(rutas["capturas"])).canonicalFile
        assertEquals(raiz, captura.canonicalFile.parentFile)
        // FileProvider usa separadores '/' de Android; Java File usa '\\' en el host Windows.
        // En ese host se verifica el contrato y XML; la resolución real se verifica en Linux/Android.
        val uri = if (File.separatorChar == '/') FileProvider.getUriForFile(context, autoridad, captura)
            else Uri.Builder().scheme("content").authority(autoridad).appendPath("capturas").appendPath(captura.name).build()
        assertEquals("content", uri.scheme)
        val intent = ActivityResultContracts.TakePicture().createIntent(context, uri)
        assertEquals(MediaStore.ACTION_IMAGE_CAPTURE, intent.action)
        @Suppress("DEPRECATION")
        assertEquals(uri, intent.getParcelableExtra<Uri>(MediaStore.EXTRA_OUTPUT))
        if (File.separatorChar == '/') {
            context.contentResolver.openOutputStream(uri)!!.use { it.write(byteArrayOf(1, 2, 3)) }
            assertEquals(3, captura.length().toInt())
        }
        archivos.descartar(captura.path)
    }

    @Test fun importacionRechazaFormatoInvalidoYArchivosMayoresAlLimite() {
        val invalido = File(context.cacheDir, "falso.pdf").apply { writeText("No es un PDF") }
        assertThrows(IllegalStateException::class.java) { archivos.importar(Uri.fromFile(invalido)) }
        invalido.delete()
        val grande = File(context.cacheDir, "grande.pdf")
        RandomAccessFile(grande, "rw").use {
            it.write("%PDF-".toByteArray()); it.setLength(ArchivoAdjuntoStore.MAX_BYTES + 1)
        }
        assertThrows(IllegalArgumentException::class.java) { archivos.importar(Uri.fromFile(grande)) }
        grande.delete()
        assertTrue(File(context.cacheDir, "adjuntos_pendientes").listFiles().orEmpty().isEmpty())
    }
}
