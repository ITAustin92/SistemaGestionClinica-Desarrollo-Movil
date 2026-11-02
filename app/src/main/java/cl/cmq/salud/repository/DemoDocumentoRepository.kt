package cl.cmq.salud.repository

import cl.cmq.salud.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

open class DemoDocumentoRepository(iniciales: List<Documento>? = null) : DocumentoRepository {
    override val usuarios = listOf(
        Usuario(1, "Ángel Quintero", "Centro Médico Quilpué", Rol.FUNCIONARIO),
        Usuario(2, "Food Fontus", "Centro Médico Quilpué", Rol.FUNCIONARIO),
        Usuario(3, "Máximo Lugo", "Centro Médico Belloto", Rol.FUNCIONARIO),
        Usuario(4, "Dr. Pedro Soto", "Centro Médico Quilpué", Rol.REVISOR),
    )
    private val datos = MutableStateFlow(iniciales ?: datosIniciales())
    override val documentos = datos.asStateFlow()
    private var siguienteId = (datos.value.maxOfOrNull { it.id } ?: 0) + 1

    protected open fun persistir(documentos: List<Documento>) {}

    protected fun restaurar(documentos: List<Documento>) {
        siguienteId = (documentos.maxOfOrNull { it.id } ?: 0) + 1
        datos.value = documentos
    }

    private fun publicar(documentos: List<Documento>) {
        // Publicar solo después de confirmar la escritura, nunca anticipar el éxito.
        persistir(documentos)
        restaurar(documentos)
    }

    @Synchronized
    override fun reiniciar() { publicar(datosIniciales()) }

    override fun documentosVisibles(usuario: Usuario): List<Documento> = datos.value.filter {
        when (usuario.rol) {
            Rol.FUNCIONARIO -> it.titularId == usuario.id
            Rol.REVISOR -> it.centro == usuario.centro && it.titularId != usuario.id
        }
    }

    override fun buscar(usuario: Usuario, id: Long) = documentosVisibles(usuario).find { it.id == id }

    @Synchronized
    override fun registrar(usuario: Usuario, titulo: String, tipo: TipoDocumento, observacion: String, adjunto: Adjunto): Long {
        require(usuario.rol == Rol.FUNCIONARIO) { "Solo un funcionario puede registrar documentos." }
        require(titulo.trim().length in 3..80) { "El título debe tener entre 3 y 80 caracteres." }
        require(observacion.length <= 500) { "La observación admite hasta 500 caracteres." }
        val id = siguienteId
        val fecha = ahora()
        publicar(listOf(Documento(id, usuario.id, usuario.nombre, usuario.centro,
            titulo.trim(), tipo, observacion.trim(), adjunto, fecha,
            historial = listOf(EventoDocumento("Documento cargado", usuario.nombre, fecha)))) + datos.value)
        return id
    }

    @Synchronized
    override fun revisar(usuario: Usuario, id: Long, decision: EstadoDocumento, observacion: String) {
        require(usuario.rol == Rol.REVISOR) { "Solo el revisor puede registrar una decisión." }
        val documento = requireNotNull(buscar(usuario, id)) { "No tienes acceso a este documento." }
        require(documento.estado == EstadoDocumento.PENDIENTE) { "El documento ya fue revisado." }
        require(decision != EstadoDocumento.PENDIENTE) { "Selecciona una decisión válida." }
        require(observacion.length <= 500) { "La observación admite hasta 500 caracteres." }
        require(decision != EstadoDocumento.RECHAZADO || observacion.isNotBlank()) { "Escribe una observación para rechazar." }
        val fecha = ahora()
        val revisado = documento.copy(estado = decision,
            revision = RevisionDocumento(usuario.nombre, observacion.trim(), fecha),
            historial = documento.historial + EventoDocumento("Documento ${decision.etiqueta.lowercase()}", usuario.nombre, fecha))
        publicar(datos.value.map { if (it.id == id) revisado else it })
    }

    private fun ahora(): String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-CL")))

    private fun datosIniciales(): List<Documento> {
        fun doc(id: Long, usuario: Usuario, titulo: String, tipo: TipoDocumento, estado: EstadoDocumento = EstadoDocumento.PENDIENTE): Documento {
            val fecha = "20 oct 2026 · 10:30"
            val revision = if (estado == EstadoDocumento.APROBADO) RevisionDocumento(usuarios.last().nombre, "Antecedente revisado en la demostración.", fecha) else null
            return Documento(id, usuario.id, usuario.nombre, usuario.centro, titulo, tipo,
                "Documento ficticio para recorrer las vistas de la aplicación.", Adjunto("documento_demo.pdf", "PDF"),
                fecha, estado, revision,
                listOf(EventoDocumento("Documento cargado", usuario.nombre, fecha)) +
                    if (revision != null) listOf(EventoDocumento("Documento aprobado", revision.revisor, fecha)) else emptyList())
        }
        return listOf(
            doc(1, usuarios[0], "Certificado de capacitación", TipoDocumento.CERTIFICADO),
            doc(2, usuarios[0], "Contrato de trabajo", TipoDocumento.CONTRATO, EstadoDocumento.APROBADO),
            doc(3, usuarios[0], "Antecedente administrativo", TipoDocumento.ANTECEDENTE),
            doc(4, usuarios[1], "Certificado de ejemplo", TipoDocumento.CERTIFICADO),
            doc(5, usuarios[2], "Documento de otro centro", TipoDocumento.ANTECEDENTE),
        )
    }
}
