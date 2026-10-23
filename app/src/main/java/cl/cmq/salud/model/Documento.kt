package cl.cmq.salud.model

enum class Rol(val etiqueta: String) { FUNCIONARIO("Funcionario"), REVISOR("Revisor") }

data class Usuario(val id: Long, val nombre: String, val centro: String, val rol: Rol)

enum class TipoDocumento(val etiqueta: String) {
    CERTIFICADO("Certificado"), CONTRATO("Contrato"), ANTECEDENTE("Antecedente administrativo")
}

enum class EstadoDocumento(val etiqueta: String) {
    PENDIENTE("Pendiente"), APROBADO("Aprobado"), RECHAZADO("Rechazado")
}

data class Adjunto(
    val nombre: String,
    val formato: String,
    val tamano: String = "PDF de ejemplo",
    val rutaLocal: String? = null,
    val mimeType: String = "application/pdf",
)

data class EventoDocumento(val titulo: String, val actor: String, val fecha: String)

data class RevisionDocumento(val revisor: String, val observacion: String, val fecha: String)

data class Documento(
    val id: Long,
    val titularId: Long,
    val titular: String,
    val centro: String,
    val titulo: String,
    val tipo: TipoDocumento,
    val observacion: String,
    val adjunto: Adjunto,
    val fechaCarga: String,
    val estado: EstadoDocumento = EstadoDocumento.PENDIENTE,
    val revision: RevisionDocumento? = null,
    val historial: List<EventoDocumento> = emptyList(),
)
