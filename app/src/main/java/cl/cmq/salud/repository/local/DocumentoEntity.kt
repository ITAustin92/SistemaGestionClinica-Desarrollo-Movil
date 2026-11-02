package cl.cmq.salud.repository.local

import androidx.room.*
import cl.cmq.salud.model.*

@Entity(tableName = "documentos")
data class DocumentoEntity(
    @PrimaryKey val id: Long,
    @ColumnInfo(name = "titular_id") val titularId: Long,
    val titular: String, val centro: String, val titulo: String, val tipo: String,
    val observacion: String,
    @ColumnInfo(name = "fecha_carga") val fechaCarga: String,
    val estado: String,
    @ColumnInfo(name = "adjunto_nombre") val adjuntoNombre: String,
    @ColumnInfo(name = "adjunto_formato") val adjuntoFormato: String,
    @ColumnInfo(name = "adjunto_tamano") val adjuntoTamano: String,
    @ColumnInfo(name = "adjunto_ruta") val adjuntoRuta: String?,
    @ColumnInfo(name = "adjunto_mime") val adjuntoMime: String,
    val revisor: String?,
    @ColumnInfo(name = "revision_observacion") val revisionObservacion: String?,
    @ColumnInfo(name = "revision_fecha") val revisionFecha: String?,
) {
    fun modelo(eventos: List<EventoEntity>) = Documento(id, titularId, titular, centro, titulo,
        TipoDocumento.valueOf(tipo), observacion,
        Adjunto(adjuntoNombre, adjuntoFormato, adjuntoTamano, adjuntoRuta, adjuntoMime), fechaCarga,
        EstadoDocumento.valueOf(estado), revisor?.let {
            RevisionDocumento(it, requireNotNull(revisionObservacion), requireNotNull(revisionFecha))
        }, eventos.sortedBy { it.posicion }.map { EventoDocumento(it.titulo, it.actor, it.fecha) })

    companion object {
        fun de(d: Documento) = DocumentoEntity(d.id, d.titularId, d.titular, d.centro, d.titulo, d.tipo.name,
            d.observacion, d.fechaCarga, d.estado.name, d.adjunto.nombre, d.adjunto.formato,
            d.adjunto.tamano, d.adjunto.rutaLocal, d.adjunto.mimeType,
            d.revision?.revisor, d.revision?.observacion, d.revision?.fecha)
    }
}

@Entity(tableName = "eventos", primaryKeys = ["documento_id", "posicion"],
    foreignKeys = [ForeignKey(entity = DocumentoEntity::class, parentColumns = ["id"],
        childColumns = ["documento_id"], onDelete = ForeignKey.CASCADE)])
data class EventoEntity(@ColumnInfo(name = "documento_id") val documentoId: Long,
    val posicion: Int, val titulo: String, val actor: String, val fecha: String)

@Entity(tableName = "configuracion")
data class ConfiguracionEntity(@PrimaryKey val clave: String, val valor: String)

data class DocumentoConEventos(@Embedded val documento: DocumentoEntity,
    @Relation(parentColumn = "id", entityColumn = "documento_id") val eventos: List<EventoEntity>)
