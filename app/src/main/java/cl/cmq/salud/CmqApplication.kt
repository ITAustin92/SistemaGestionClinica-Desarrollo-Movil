package cl.cmq.salud

import android.app.Application
import cl.cmq.salud.repository.SQLiteDocumentoRepository
import cl.cmq.salud.repository.ArchivoAdjuntoStore
import cl.cmq.salud.repository.DocumentoRepository

class CmqApplication : Application() {
    val archivos: ArchivoAdjuntoStore by lazy { ArchivoAdjuntoStore(this) }
    val documentoRepository: DocumentoRepository by lazy { SQLiteDocumentoRepository(this, archivos) }
}
