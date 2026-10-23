package cl.cmq.salud.ui.documentform

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.cmq.salud.model.TipoDocumento
import cl.cmq.salud.ui.components.*
import cl.cmq.salud.ui.theme.*
import cl.cmq.salud.viewmodel.FormularioUiState

@Composable
fun RegistroDocumentoScreen(state: FormularioUiState, onTitulo: (String) -> Unit,
    onTipo: (TipoDocumento) -> Unit, onObservacion: (String) -> Unit,
    onAdjuntarDemo: () -> Unit, onAdjuntar: () -> Unit, onCamara: () -> Unit, onVistaPrevia: () -> Unit,
    onGuardar: () -> Unit, onVolver: () -> Unit) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    val volver = { if (state.tieneCambios) confirmarSalida = true else onVolver() }
    BackHandler { volver() }
    if (confirmarSalida) AlertDialog(
        onDismissRequest = { confirmarSalida = false }, title = { Text("¿Descartar los cambios?") },
        text = { Text("Los datos de este formulario todavía no se han guardado.") },
        confirmButton = { TextButton(onClick = { confirmarSalida = false; onVolver() }) { Text("Descartar") } },
        dismissButton = { TextButton(onClick = { confirmarSalida = false }) { Text("Seguir editando") } })

    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Encabezado(onVolver = volver)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Nuevo documento", style = MaterialTheme.typography.headlineMedium)
            Text("Completa los datos y adjunta tu archivo", color = GrisTexto)
        }
        CampoFormulario(state.titulo, "Título del documento *", onTitulo, error = state.errores["titulo"])
        SelectorOpciones("Tipo de documento *", state.tipo?.etiqueta ?: "Selecciona un tipo", TipoDocumento.entries,
            { it.etiqueta }, onTipo, error = state.errores["tipo"])
        CampoFormulario(state.observacion, "Observación", onObservacion, multilinea = true,
            ayuda = "Opcional · Máximo 500 caracteres", error = state.errores["observacion"])
        Text("Adjunto *", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onAdjuntar, modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = MaterialTheme.shapes.small) {
                Icon(Icons.Outlined.AttachFile, null, Modifier.size(20.dp)); Spacer(Modifier.width(5.dp)); Text("Elegir archivo")
            }
            OutlinedButton(onClick = onCamara, modifier = Modifier.weight(1f).heightIn(min = 56.dp), shape = MaterialTheme.shapes.small) {
                Icon(Icons.Outlined.CameraAlt, null, Modifier.size(20.dp)); Spacer(Modifier.width(5.dp)); Text("Escanear")
            }
        }
        if (state.adjunto != null) {
            Row(Modifier.fillMaxWidth().background(Aqua, MaterialTheme.shapes.medium).padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Description, null, tint = Turquesa)
                Column(Modifier.weight(1f)) {
                    Text(state.adjunto.nombre, style = MaterialTheme.typography.titleMedium)
                    Text("${state.adjunto.formato} · ${state.adjunto.tamano}", color = GrisTexto)
                }
                TextButton(onClick = onAdjuntar) { Text("Cambiar") }
            }
            if (state.adjunto.rutaLocal != null) {
                if (state.adjunto.mimeType.startsWith("image/")) {
                    ImagenAdjunto(state.adjunto, Modifier.fillMaxWidth().height(220.dp))
                }
                TextButton(onClick = onVistaPrevia) { Text("Revisar adjunto") }
            }
        }
        state.errores["adjunto"]?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Text("PDF, JPG o PNG · Hasta 10 MiB", color = GrisTexto, style = MaterialTheme.typography.bodySmall)
        Text("Escanea una página con la cámara y confirma que el texto se lea antes de guardarla. Se almacena como imagen, sin reconocimiento de texto ni recorte automático.", color = GrisTexto, style = MaterialTheme.typography.bodySmall)
        TextButton(onClick = onAdjuntarDemo) { Text("Generar PDF de ejemplo al guardar") }
        BotonPrincipal("Guardar documento", onGuardar)
        PieDemostracion()
    }
}
