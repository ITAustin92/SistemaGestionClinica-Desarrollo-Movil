package cl.cmq.salud.repository

import cl.cmq.salud.model.*
import kotlinx.coroutines.flow.StateFlow

interface DocumentoRepository {
    val usuarios: List<Usuario>
    val documentos: StateFlow<List<Documento>>
    fun documentosVisibles(usuario: Usuario): List<Documento>
    fun buscar(usuario: Usuario, id: Long): Documento?
    fun registrar(usuario: Usuario, titulo: String, tipo: TipoDocumento, observacion: String, adjunto: AdjuntoDemo): Long
    fun revisar(usuario: Usuario, id: Long, decision: EstadoDocumento, observacion: String)
}
