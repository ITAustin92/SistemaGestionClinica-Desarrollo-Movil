package cl.cmq.salud.repository.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cl.cmq.salud.model.Documento

@Database(entities = [DocumentoEntity::class, EventoEntity::class, ConfiguracionEntity::class],
    version = 2, exportSchema = true)
abstract class CmqRoomDatabase : RoomDatabase() {
    abstract fun documentoDao(): DocumentoDao
}

/** Mantiene el contrato del repositorio; toda lectura y escritura pasa por el DAO de Room. */
class DocumentoDatabase(context: Context, nombre: String = "cmq_documentos.db") : AutoCloseable {
    private val room = Room.databaseBuilder(context.applicationContext, CmqRoomDatabase::class.java, nombre)
        .addMigrations(MIGRACION_1_2).build()
    private val dao get() = room.documentoDao()
    val writableDatabase: SupportSQLiteDatabase get() = room.openHelper.writableDatabase
    fun inicializada() = dao.inicializada()
    fun leer(): List<Documento> = dao.listar().map { it.documento.modelo(it.eventos) }
    fun guardar(documentos: List<Documento>) = dao.guardar(documentos)
    override fun close() = room.close()

    companion object {
        /** SQLiteOpenHelper v1 -> Room v2. Copia todas las filas y conserva las rutas privadas. */
        val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""CREATE TABLE documentos_room (
                    id INTEGER NOT NULL PRIMARY KEY, titular_id INTEGER NOT NULL, titular TEXT NOT NULL,
                    centro TEXT NOT NULL, titulo TEXT NOT NULL, tipo TEXT NOT NULL,
                    observacion TEXT NOT NULL, fecha_carga TEXT NOT NULL, estado TEXT NOT NULL,
                    adjunto_nombre TEXT NOT NULL, adjunto_formato TEXT NOT NULL, adjunto_tamano TEXT NOT NULL,
                    adjunto_ruta TEXT, adjunto_mime TEXT NOT NULL,
                    revisor TEXT, revision_observacion TEXT, revision_fecha TEXT)""")
                db.execSQL("INSERT INTO documentos_room SELECT * FROM documentos")
                db.execSQL("CREATE TEMP TABLE eventos_respaldo AS SELECT * FROM eventos")
                db.execSQL("CREATE TABLE configuracion_room (clave TEXT NOT NULL PRIMARY KEY, valor TEXT NOT NULL)")
                db.execSQL("INSERT INTO configuracion_room SELECT clave, valor FROM configuracion")
                db.execSQL("DROP TABLE eventos")
                db.execSQL("DROP TABLE documentos")
                db.execSQL("DROP TABLE configuracion")
                db.execSQL("ALTER TABLE documentos_room RENAME TO documentos")
                // Crear la FK tras renombrar el padre funciona también en SQLite de Android 8.
                db.execSQL("""CREATE TABLE eventos (
                    documento_id INTEGER NOT NULL, posicion INTEGER NOT NULL, titulo TEXT NOT NULL,
                    actor TEXT NOT NULL, fecha TEXT NOT NULL, PRIMARY KEY(documento_id, posicion),
                    FOREIGN KEY(documento_id) REFERENCES documentos(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
                db.execSQL("INSERT INTO eventos SELECT * FROM eventos_respaldo")
                db.execSQL("DROP TABLE eventos_respaldo")
                db.execSQL("ALTER TABLE configuracion_room RENAME TO configuracion")
            }
        }
    }
}
