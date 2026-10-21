package cl.cmq.salud

import android.app.Application
import cl.cmq.salud.repository.DemoDocumentoRepository
import cl.cmq.salud.repository.DocumentoRepository

class CmqApplication : Application() {
    // Una instancia compartida mientras viva el proceso. Room se incorporará después.
    val documentoRepository: DocumentoRepository by lazy { DemoDocumentoRepository() }
}
