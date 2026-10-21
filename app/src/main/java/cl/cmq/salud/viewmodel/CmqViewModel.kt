package cl.cmq.salud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.cmq.salud.model.*
import cl.cmq.salud.repository.DocumentoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class FormularioUiState(
    val titulo: String = "", val tipo: TipoDocumento? = null, val observacion: String = "",
    val adjunto: AdjuntoDemo? = null, val errores: Map<String, String> = emptyMap(),
) {
    val tieneCambios: Boolean get() = titulo.isNotBlank() || tipo != null || observacion.isNotBlank() || adjunto != null
}

data class CmqUiState(
    val registros: List<Documento> = emptyList(),
    val usuario: Usuario? = null, val usuarioSeleccionadoId: Long? = null,
    val busqueda: String = "", val filtroTipo: TipoDocumento? = null, val filtroEstado: EstadoDocumento? = null,
    val formulario: FormularioUiState = FormularioUiState(),
    val observacionRevision: String = "", val errorRevision: String? = null,
    val mensaje: String? = null,
)

class CmqViewModel(private val repository: DocumentoRepository) : ViewModel() {
    private val datos = MutableStateFlow(CmqUiState())
    val usuarios = repository.usuarios
    // El estado observable del repositorio fuerza la actualización de listado y detalle.
    val uiState = combine(datos, repository.documentos) { estado, documentos -> estado.copy(registros = documentos) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, datos.value)
    val documentos = repository.documentos

    fun seleccionarUsuario(id: Long) { datos.update { it.copy(usuarioSeleccionadoId = id) } }
    fun entrar(): Usuario? {
        val usuario = usuarios.find { it.id == datos.value.usuarioSeleccionadoId } ?: return null
        datos.value = CmqUiState(usuario = usuario, usuarioSeleccionadoId = usuario.id)
        return usuario
    }
    fun salir() { datos.value = CmqUiState() }
    fun buscar(texto: String) { datos.update { it.copy(busqueda = texto) } }
    fun filtrarTipo(tipo: TipoDocumento?) { datos.update { it.copy(filtroTipo = tipo) } }
    fun filtrarEstado(estado: EstadoDocumento?) { datos.update { it.copy(filtroEstado = estado) } }
    fun limpiarFiltros() { datos.update { it.copy(busqueda = "", filtroTipo = null, filtroEstado = null) } }
    fun visibles(): List<Documento> {
        val state = datos.value
        val usuario = state.usuario ?: return emptyList()
        return repository.documentosVisibles(usuario).filter { doc ->
            (usuario.rol != Rol.REVISOR || doc.estado == EstadoDocumento.PENDIENTE) &&
                doc.titulo.contains(state.busqueda.trim(), ignoreCase = true) &&
                (state.filtroTipo == null || doc.tipo == state.filtroTipo) &&
                (state.filtroEstado == null || doc.estado == state.filtroEstado)
        }
    }
    fun detalle(id: Long): Documento? = datos.value.usuario?.let { repository.buscar(it, id) }
    fun nuevoDocumento() { datos.update { it.copy(formulario = FormularioUiState()) } }
    private fun formulario(cambio: (FormularioUiState) -> FormularioUiState) {
        datos.update { it.copy(formulario = cambio(it.formulario)) }
    }
    fun cambiarTitulo(valor: String) { formulario { it.copy(titulo = valor, errores = it.errores - "titulo") } }
    fun cambiarTipo(tipo: TipoDocumento) { formulario { it.copy(tipo = tipo, errores = it.errores - "tipo") } }
    fun cambiarObservacion(valor: String) { formulario { it.copy(observacion = valor, errores = it.errores - "observacion") } }
    fun adjuntarDemo() { formulario { it.copy(adjunto = AdjuntoDemo("certificado_demo.pdf", "PDF"), errores = it.errores - "adjunto") } }
    fun guardar(): Long? {
        val usuario = datos.value.usuario ?: return null
        val form = datos.value.formulario
        val errores = buildMap {
            if (form.titulo.trim().length !in 3..80) put("titulo", "El título debe tener entre 3 y 80 caracteres.")
            if (form.tipo == null) put("tipo", "Selecciona un tipo de documento.")
            if (form.observacion.length > 500) put("observacion", "Usa máximo 500 caracteres.")
            if (form.adjunto == null) put("adjunto", "Agrega el adjunto de demostración.")
        }
        formulario { it.copy(errores = errores) }
        if (errores.isNotEmpty()) return null
        return runCatching {
            repository.registrar(usuario, form.titulo, requireNotNull(form.tipo), form.observacion, requireNotNull(form.adjunto))
        }.onSuccess {
            datos.update { it.copy(formulario = FormularioUiState(), mensaje = "Documento guardado en la demostración.") }
        }.onFailure { mostrarMensaje(it.message ?: "No se pudo guardar el documento.") }.getOrNull()
    }
    fun abrirDetalle() { datos.update { it.copy(observacionRevision = "", errorRevision = null) } }
    fun cambiarRevision(valor: String) { datos.update { it.copy(observacionRevision = valor, errorRevision = null) } }
    fun revisar(id: Long, decision: EstadoDocumento) {
        val usuario = datos.value.usuario ?: return
        runCatching { repository.revisar(usuario, id, decision, datos.value.observacionRevision) }
            .onSuccess { datos.update { it.copy(errorRevision = null, mensaje = "Revisión registrada: ${decision.etiqueta.lowercase()}.") } }
            .onFailure { error -> datos.update { it.copy(errorRevision = error.message ?: "No se pudo revisar.") } }
    }
    fun mostrarMensaje(mensaje: String) { datos.update { it.copy(mensaje = mensaje) } }
    fun consumirMensaje() { datos.update { it.copy(mensaje = null) } }

    companion object {
        fun factory(repository: DocumentoRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(CmqViewModel::class.java))
                return CmqViewModel(repository) as T
            }
        }
    }
}
