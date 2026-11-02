package cl.cmq.salud.repository.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import cl.cmq.salud.model.*

/** SQLite v1. Los adjuntos son archivos privados; la base guarda solo su ruta. */
class DocumentoDatabase(context: Context, nombre: String = "cmq_documentos.db") :
    SQLiteOpenHelper(context.applicationContext, nombre, null, 1) {

    override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""CREATE TABLE documentos (
            id INTEGER PRIMARY KEY, titular_id INTEGER NOT NULL, titular TEXT NOT NULL,
            centro TEXT NOT NULL, titulo TEXT NOT NULL, tipo TEXT NOT NULL,
            observacion TEXT NOT NULL, fecha_carga TEXT NOT NULL, estado TEXT NOT NULL,
            adjunto_nombre TEXT NOT NULL, adjunto_formato TEXT NOT NULL, adjunto_tamano TEXT NOT NULL,
            adjunto_ruta TEXT, adjunto_mime TEXT NOT NULL,
            revisor TEXT, revision_observacion TEXT, revision_fecha TEXT)""")
        db.execSQL("""CREATE TABLE eventos (
            documento_id INTEGER NOT NULL REFERENCES documentos(id) ON DELETE CASCADE,
            posicion INTEGER NOT NULL, titulo TEXT NOT NULL, actor TEXT NOT NULL, fecha TEXT NOT NULL,
            PRIMARY KEY (documento_id, posicion))""")
        db.execSQL("CREATE TABLE configuracion (clave TEXT PRIMARY KEY, valor TEXT NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Falta una migración de SQLite de $oldVersion a $newVersion.")
    }

    fun inicializada(): Boolean = readableDatabase.rawQuery(
        "SELECT 1 FROM configuracion WHERE clave = ?", arrayOf("inicializada")
    ).use { it.moveToFirst() }

    fun leer(): List<Documento> {
        val db = readableDatabase
        return db.query("documentos", null, null, null, null, null, "id DESC").use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
                    val historial = db.query("eventos", null, "documento_id = ?", arrayOf(id.toString()),
                        null, null, "posicion ASC").use { eventos ->
                        buildList {
                            while (eventos.moveToNext()) add(EventoDocumento(
                                eventos.texto("titulo"), eventos.texto("actor"), eventos.texto("fecha")))
                        }
                    }
                    add(Documento(id, cursor.getLong(cursor.getColumnIndexOrThrow("titular_id")),
                        cursor.texto("titular"), cursor.texto("centro"), cursor.texto("titulo"),
                        TipoDocumento.valueOf(cursor.texto("tipo")), cursor.texto("observacion"),
                        Adjunto(cursor.texto("adjunto_nombre"), cursor.texto("adjunto_formato"),
                            cursor.texto("adjunto_tamano"), cursor.opcional("adjunto_ruta"), cursor.texto("adjunto_mime")),
                        cursor.texto("fecha_carga"), EstadoDocumento.valueOf(cursor.texto("estado")),
                        cursor.opcional("revisor")?.let {
                            RevisionDocumento(it, cursor.texto("revision_observacion"), cursor.texto("revision_fecha"))
                        }, historial))
                }
            }
        }
    }

    /** Una transacción conserva documentos, decisiones e historial como una sola unidad. */
    fun guardar(documentos: List<Documento>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.delete("documentos", null, null) // eventos se eliminan mediante la FK.
            documentos.forEach { documento ->
                db.insertOrThrow("documentos", null, ContentValues().apply {
                    put("id", documento.id); put("titular_id", documento.titularId)
                    put("titular", documento.titular); put("centro", documento.centro)
                    put("titulo", documento.titulo); put("tipo", documento.tipo.name)
                    put("observacion", documento.observacion); put("fecha_carga", documento.fechaCarga)
                    put("estado", documento.estado.name)
                    put("adjunto_nombre", documento.adjunto.nombre); put("adjunto_formato", documento.adjunto.formato)
                    put("adjunto_tamano", documento.adjunto.tamano); put("adjunto_ruta", documento.adjunto.rutaLocal)
                    put("adjunto_mime", documento.adjunto.mimeType)
                    put("revisor", documento.revision?.revisor)
                    put("revision_observacion", documento.revision?.observacion)
                    put("revision_fecha", documento.revision?.fecha)
                })
                documento.historial.forEachIndexed { posicion, evento ->
                    db.insertOrThrow("eventos", null, ContentValues().apply {
                        put("documento_id", documento.id); put("posicion", posicion)
                        put("titulo", evento.titulo); put("actor", evento.actor); put("fecha", evento.fecha)
                    })
                }
            }
            db.insertWithOnConflict("configuracion", null, ContentValues().apply {
                put("clave", "inicializada"); put("valor", "1")
            }, SQLiteDatabase.CONFLICT_REPLACE)
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun Cursor.texto(nombre: String): String = getString(getColumnIndexOrThrow(nombre))
    private fun Cursor.opcional(nombre: String): String? = getColumnIndexOrThrow(nombre).let {
        if (isNull(it)) null else getString(it)
    }
}
