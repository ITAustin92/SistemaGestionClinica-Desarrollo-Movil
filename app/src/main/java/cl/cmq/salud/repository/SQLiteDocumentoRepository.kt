package cl.cmq.salud.repository

import android.content.Context
import cl.cmq.salud.model.*
import cl.cmq.salud.repository.local.DocumentoDatabase
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Reutiliza las reglas del prototipo y confirma cada cambio en SQLite antes de publicarlo. */
class SQLiteDocumentoRepository(context: Context, private val archivos: ArchivoAdjuntoStore,
    nombreBase: String = "cmq_documentos.db") : DemoDocumentoRepository(emptyList()), AutoCloseable {
    private val database = DocumentoDatabase(context, nombreBase)
    private var lista = false

    @Synchronized
    override fun cargar() {
        if (lista) return
        val inicializada = database.inicializada()
        val anteriores = if (inicializada) database.leer() else DemoDocumentoRepository().documentos.value
        val actualizados = anteriores.map(::actualizarNombres)
        guardarConEjemplos(actualizados, forzar = !inicializada || actualizados != anteriores)
        // Los PDF de muestra sustituidos se eliminan solo después de confirmar SQLite.
        anteriores.zip(actualizados).forEach { (anterior, nuevo) ->
            if (anterior.adjunto.rutaLocal != null && nuevo.adjunto.rutaLocal == null) {
                archivos.eliminarGuardado(anterior.adjunto.rutaLocal)
            }
        }
        lista = true
    }

    /** Migra las etiquetas antiguas sin borrar registros, revisiones ni archivos del usuario. */
    private fun actualizarNombres(documento: Documento): Documento {
        val equivalencias = mapOf("Funcionario Demo A" to usuarios[0].nombre, "Funcionario Demo B" to usuarios[1].nombre,
            "Funcionario Demo C" to usuarios[2].nombre, "Revisor Demo A" to usuarios[3].nombre,
            "Dr. Nombre Genérico" to usuarios[3].nombre,
            "Centro Demo A" to "Centro Médico Quilpué", "Centro Demo B" to "Centro Médico Belloto")
        fun nombre(valor: String) = equivalencias.entries.fold(valor) { texto, (anterior, nuevo) -> texto.replace(anterior, nuevo) }
        val titular = usuarios.find { it.id == documento.titularId }?.nombre ?: nombre(documento.titular)
        val centro = nombre(documento.centro)
        val titulo = nombre(documento.titulo)
        val esPdfGenerado = documento.adjunto.nombre == "${documento.tipo.name.lowercase(Locale.ROOT)}_de_ejemplo.pdf"
        val renovarPdf = esPdfGenerado && (titular != documento.titular || centro != documento.centro || titulo != documento.titulo)
        return documento.copy(
            titular = titular, centro = centro, titulo = titulo, observacion = nombre(documento.observacion),
            adjunto = if (renovarPdf) documento.adjunto.copy(rutaLocal = null) else documento.adjunto,
            revision = documento.revision?.let { it.copy(revisor = nombre(it.revisor), observacion = nombre(it.observacion)) },
            historial = documento.historial.map { it.copy(actor = nombre(it.actor), titulo = nombre(it.titulo)) })
    }

    private fun ejemploGuardado(documento: Documento): Adjunto {
        val temporal = archivos.crearEjemplo(documento)
        return try { archivos.conservar(temporal) } finally { archivos.descartar(temporal.rutaLocal) }
    }

    private fun guardarConEjemplos(documentos: List<Documento>, forzar: Boolean = false) {
        val creados = mutableListOf<String>()
        try {
            val completos = documentos.map { documento ->
                if (documento.adjunto.rutaLocal != null) documento else {
                    val adjunto = ejemploGuardado(documento)
                    adjunto.rutaLocal?.let(creados::add)
                    documento.copy(adjunto = adjunto)
                }
            }.sortedByDescending { it.id }
            if (forzar || creados.isNotEmpty()) database.guardar(completos)
            restaurar(completos)
        } catch (error: Exception) {
            creados.forEach(archivos::eliminarGuardado)
            throw error
        }
    }

    override fun persistir(documentos: List<Documento>) {
        check(lista) { "Espera a que termine la carga de documentos." }
        database.guardar(documentos)
    }

    @Synchronized
    override fun registrar(usuario: Usuario, titulo: String, tipo: TipoDocumento, observacion: String, adjunto: Adjunto): Long {
        val definitivo = if (adjunto.rutaLocal != null) archivos.conservar(adjunto) else {
            val fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-CL")))
            ejemploGuardado(Documento((documentos.value.maxOfOrNull { it.id } ?: 0) + 1, usuario.id,
                usuario.nombre, usuario.centro, titulo.trim(), tipo, observacion, adjunto, fecha))
        }
        try {
            val id = super.registrar(usuario, titulo, tipo, observacion, definitivo)
            archivos.descartar(adjunto.rutaLocal)
            return id
        } catch (error: Exception) {
            archivos.eliminarGuardado(definitivo.rutaLocal)
            throw error
        }
    }

    @Synchronized
    override fun reiniciar() {
        check(lista) { "Espera a que termine la carga de documentos." }
        guardarConEjemplos(DemoDocumentoRepository().documentos.value, forzar = true)
        archivos.limpiar(documentos.value.mapNotNull { it.adjunto.rutaLocal }.toSet())
    }

    override fun close() = database.close()
}
