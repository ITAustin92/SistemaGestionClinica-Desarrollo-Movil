package cl.cmq.salud.ui.documentdetail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.cmq.salud.model.*
import cl.cmq.salud.ui.components.*
import cl.cmq.salud.ui.theme.*

@Composable
fun DetalleDocumentoScreen(documento: Documento?, usuario: Usuario, observacionRevision: String,
    errorRevision: String?, onObservacion: (String) -> Unit, onAprobar: () -> Unit, onRechazar: () -> Unit,
    onAbrirAdjunto: () -> Unit, onVolver: () -> Unit, onSalir: () -> Unit) {
    Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Encabezado(onVolver = onVolver, onSalir = onSalir)
        Text("Detalle del documento", style = MaterialTheme.typography.headlineMedium)
        if (documento == null) {
            SinDocumentos("Documento no disponible", "El registro no existe o no está disponible para este perfil.")
            BotonPrincipal("Volver", onVolver)
        } else {
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color.White), elevation = CardDefaults.elevatedCardElevation(2.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    IconoCircular(Icons.Outlined.Description)
                    Text(documento.titulo, style = MaterialTheme.typography.titleLarge)
                    EtiquetaEstado(documento.estado)
                    HorizontalDivider()
                    FilaDato("Funcionario", documento.titular)
                    FilaDato("Centro", documento.centro)
                    FilaDato("Tipo", documento.tipo.etiqueta)
                    FilaDato("Fecha de carga", documento.fechaCarga)
                    if (documento.observacion.isNotBlank()) FilaDato("Observación", documento.observacion)
                    OutlinedButton(onClick = onAbrirAdjunto, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = CircleShape) {
                        Icon(Icons.Outlined.Description, null); Spacer(Modifier.width(9.dp)); Text("Abrir adjunto")
                    }
                }
            }
            if (usuario.rol == Rol.REVISOR && documento.estado == EstadoDocumento.PENDIENTE) {
                ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color.White), elevation = CardDefaults.elevatedCardElevation(2.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("Revisión del documento", style = MaterialTheme.typography.titleLarge)
                        Text(usuario.nombre, color = GrisTexto)
                        CampoFormulario(observacionRevision, "Observación de revisión", onObservacion, multilinea = true,
                            ayuda = "Obligatoria para rechazar", error = errorRevision)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(onClick = onAprobar, modifier = Modifier.weight(1f).heightIn(min = 50.dp), shape = CircleShape) { Text("Aprobar") }
                            OutlinedButton(onClick = onRechazar, modifier = Modifier.weight(1f).heightIn(min = 50.dp), shape = CircleShape,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Rechazar") }
                        }
                    }
                }
            }
            AnimatedVisibility(visible = documento.revision != null) {
                documento.revision?.let { revision ->
                    Column(Modifier.fillMaxWidth().background(Aqua, MaterialTheme.shapes.medium).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Revisión registrada", style = MaterialTheme.typography.titleMedium)
                        Text("${documento.estado.etiqueta} · ${revision.revisor}")
                        Text(revision.fecha, color = GrisTexto)
                        if (revision.observacion.isNotEmpty()) Text(revision.observacion)
                    }
                }
            }
            ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color.White), elevation = CardDefaults.elevatedCardElevation(2.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Historial", style = MaterialTheme.typography.titleLarge)
                    documento.historial.forEach { evento ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                            Box(Modifier.padding(top = 6.dp).size(10.dp).background(Turquesa, CircleShape))
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(evento.titulo, style = MaterialTheme.typography.titleMedium)
                                Text("${evento.actor} · ${evento.fecha}", color = GrisTexto, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
        PieDemostracion()
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(etiqueta, color = GrisTexto, style = MaterialTheme.typography.bodySmall)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}
