package cl.cmq.salud.repository.local

import androidx.room.*
import cl.cmq.salud.model.Documento

/** Operaciones síncronas: el ViewModel las ejecuta en Dispatchers.IO. */
@Dao
abstract class DocumentoDao {
    @Transaction @Query("SELECT * FROM documentos ORDER BY id DESC")
    abstract fun listar(): List<DocumentoConEventos>

    @Insert abstract fun insertar(documento: DocumentoEntity)
    @Update abstract fun actualizar(documento: DocumentoEntity)
    @Query("DELETE FROM documentos WHERE id = :id") abstract fun eliminar(id: Long)
    @Query("DELETE FROM eventos") abstract fun limpiarEventos()
    @Insert abstract fun insertarEventos(eventos: List<EventoEntity>)
    @Query("SELECT EXISTS(SELECT 1 FROM configuracion WHERE clave = 'inicializada')")
    abstract fun inicializada(): Boolean
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun configurar(configuracion: ConfiguracionEntity)

    /** Cambios, historial y marca inicial se confirman juntos antes de actualizar la UI. */
    @Transaction
    open fun guardar(documentos: List<Documento>) {
        val anteriores = listar().associate { it.documento.id to it.documento }
        val ids = documentos.map { it.id }.toSet()
        anteriores.keys.filter { it !in ids }.forEach(::eliminar)
        documentos.forEach {
            val entidad = DocumentoEntity.de(it)
            if (entidad.id !in anteriores) insertar(entidad)
            else if (entidad != anteriores[entidad.id]) actualizar(entidad)
        }
        limpiarEventos()
        insertarEventos(documentos.flatMap { d -> d.historial.mapIndexed { i, e ->
            EventoEntity(d.id, i, e.titulo, e.actor, e.fecha)
        } })
        configurar(ConfiguracionEntity("inicializada", "1"))
    }
}
