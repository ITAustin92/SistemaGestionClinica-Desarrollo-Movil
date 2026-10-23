package cl.cmq.salud.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cl.cmq.salud.model.*
import cl.cmq.salud.repository.ArchivoAdjuntoStore
import cl.cmq.salud.repository.DocumentoRepository
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FormularioUiState(
    val titulo: String = "", val tipo: TipoDocumento? = null, val observacion: String = "",
    val adjunto: Adjunto? = null, val errores: Map<String, String> = emptyMap(),
) {
    val tieneCambios: Boolean get() = titulo.isNotBlank() || tipo != null || observacion.isNotBlank() || adjunto != null
}

data class CmqUiState(
    val registros: List<Documento> = emptyList(),
    val usuario: Usuario? = null, val usuarioSeleccionadoId: Long? = null,
    val busqueda: String = "", val filtroTipo: TipoDocumento? = null, val filtroEstado: EstadoDocumento? = null,
    val formulario: FormularioUiState = FormularioUiState(),
    val observacionRevision: String = "", val errorRevision: String? = null,
    val mensaje: String? = null, val cargando: Boolean = true, val ocupado: Boolean = false,
    val errorCarga: String? = null,
)

class CmqViewModel(private val repository: DocumentoRepository, private val archivos: ArchivoAdjuntoStore) : ViewModel() {
    private val datos = MutableStateFlow(CmqUiState())
    val usuarios = repository.usuarios
    val uiState = combine(datos, repository.documentos) { estado, documentos -> estado.copy(registros = documentos) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, datos.value)

    init { cargar() }

    fun cargar() {
        if (datos.value.ocupado) return
        datos.update { it.copy(cargando = true, errorCarga = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { repository.cargar() }
                datos.update { it.copy(cargando = false) }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                datos.update { it.copy(cargando = false, errorCarga = "No se pudo abrir la base local. Reintenta la carga.") }
            }
        }
    }

    fun seleccionarUsuario(id: Long) { datos.update { it.copy(usuarioSeleccionadoId = id) } }
    fun entrar(): Usuario? {
        if (datos.value.cargando || datos.value.errorCarga != null) return null
        val usuario = usuarios.find { it.id == datos.value.usuarioSeleccionadoId } ?: return null
        datos.value = CmqUiState(usuario = usuario, usuarioSeleccionadoId = usuario.id, cargando = false)
        return usuario
    }
    fun salir() {
        descartarFormulario()
        datos.value = CmqUiState(cargando = false)
    }
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
    fun nuevoDocumento() = descartarFormulario()
    fun descartarFormulario() {
        val ruta = datos.value.formulario.adjunto?.rutaLocal
        datos.update { it.copy(formulario = FormularioUiState()) }
        viewModelScope.launch(Dispatchers.IO) { archivos.descartar(ruta) }
    }
    private fun formulario(cambio: (FormularioUiState) -> FormularioUiState) {
        datos.update { it.copy(formulario = cambio(it.formulario)) }
    }
    fun cambiarTitulo(valor: String) { formulario { it.copy(titulo = valor, errores = it.errores - "titulo") } }
    fun cambiarTipo(tipo: TipoDocumento) { formulario { it.copy(tipo = tipo, errores = it.errores - "tipo") } }
    fun cambiarObservacion(valor: String) { formulario { it.copy(observacion = valor, errores = it.errores - "observacion") } }

    private fun adjuntar(adjunto: Adjunto) {
        val anterior = datos.value.formulario.adjunto?.rutaLocal
        formulario { it.copy(adjunto = adjunto, errores = it.errores - "adjunto") }
        viewModelScope.launch(Dispatchers.IO) { archivos.descartar(anterior) }
    }
    fun adjuntarDemo() { adjuntar(Adjunto("documento_de_ejemplo.pdf", "PDF", "Se genera al guardar")) }
    fun importar(uri: Uri) {
        if (datos.value.usuario?.rol != Rol.FUNCIONARIO) return
        tarea({ archivos.importar(uri) }, ::adjuntar)
    }
    fun prepararCaptura(onPreparada: (String) -> Unit) {
        tarea({ archivos.crearCaptura().absolutePath }, onPreparada)
    }
    fun recibirCaptura(ruta: String, correcta: Boolean) {
        if (!correcta || datos.value.usuario?.rol != Rol.FUNCIONARIO) {
            cancelarCaptura(ruta)
            if (!correcta) mostrarMensaje("Captura cancelada. El adjunto anterior se conserva.")
            return
        }
        tarea({
            try { archivos.captura(ruta) } catch (error: Exception) { archivos.descartar(ruta); throw error }
        }, ::adjuntar)
    }
    fun cancelarCaptura(ruta: String) {
        viewModelScope.launch(Dispatchers.IO) { archivos.descartar(ruta) }
    }
    fun guardar(onGuardado: (Long) -> Unit) {
        val usuario = datos.value.usuario ?: return
        val form = datos.value.formulario
        val errores = buildMap {
            if (form.titulo.trim().length !in 3..80) put("titulo", "El título debe tener entre 3 y 80 caracteres.")
            if (form.tipo == null) put("tipo", "Selecciona un tipo de documento.")
            if (form.observacion.length > 500) put("observacion", "Usa máximo 500 caracteres.")
            if (form.adjunto == null) put("adjunto", "Escanea o selecciona un documento.")
        }
        formulario { it.copy(errores = errores) }
        if (errores.isNotEmpty()) return
        tarea({ repository.registrar(usuario, form.titulo, requireNotNull(form.tipo), form.observacion, requireNotNull(form.adjunto)) }) { id ->
            datos.update { it.copy(formulario = FormularioUiState(), mensaje = "Documento guardado en el dispositivo.") }
            onGuardado(id)
        }
    }
    fun abrirDetalle() { datos.update { it.copy(observacionRevision = "", errorRevision = null) } }
    fun cambiarRevision(valor: String) { datos.update { it.copy(observacionRevision = valor, errorRevision = null) } }
    fun revisar(id: Long, decision: EstadoDocumento) {
        val usuario = datos.value.usuario ?: return
        val observacion = datos.value.observacionRevision
        if (decision == EstadoDocumento.RECHAZADO && observacion.isBlank()) {
            datos.update { it.copy(errorRevision = "Escribe una observación para rechazar.") }; return
        }
        if (observacion.length > 500) {
            datos.update { it.copy(errorRevision = "La observación admite hasta 500 caracteres.") }; return
        }
        tarea({ repository.revisar(usuario, id, decision, observacion) }) {
            datos.update { it.copy(errorRevision = null, mensaje = "Revisión guardada: ${decision.etiqueta.lowercase()}.") }
        }
    }
    fun reiniciar() {
        tarea({ repository.reiniciar() }) {
            datos.value = CmqUiState(cargando = false, mensaje = "Demostración reiniciada. Se restauraron los documentos de ejemplo.")
        }
    }

    /** Toda operación de archivos o SQLite se ejecuta fuera del hilo de la interfaz. */
    private fun <T> tarea(accion: () -> T, exito: (T) -> Unit) {
        if (datos.value.ocupado || datos.value.cargando || datos.value.errorCarga != null) return
        datos.update { it.copy(ocupado = true) }
        viewModelScope.launch {
            try {
                val resultado = withContext(Dispatchers.IO) { accion() }
                datos.update { it.copy(ocupado = false) }
                exito(resultado)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                datos.update { it.copy(ocupado = false, mensaje = error.message ?: "No se pudo completar la operación.") }
            }
        }
    }
    fun mostrarMensaje(mensaje: String) { datos.update { it.copy(mensaje = mensaje) } }
    fun consumirMensaje() { datos.update { it.copy(mensaje = null) } }

    companion object {
        fun factory(repository: DocumentoRepository, archivos: ArchivoAdjuntoStore): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(CmqViewModel::class.java))
                return CmqViewModel(repository, archivos) as T
            }
        }
    }
}
