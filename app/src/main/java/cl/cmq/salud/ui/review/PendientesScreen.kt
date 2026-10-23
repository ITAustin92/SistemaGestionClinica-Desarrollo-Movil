package cl.cmq.salud.ui.review

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.cmq.salud.model.Documento
import cl.cmq.salud.model.Usuario
import cl.cmq.salud.model.EstadoDocumento
import cl.cmq.salud.ui.components.*
import cl.cmq.salud.ui.theme.GrisTexto

@Composable
fun PendientesScreen(usuario: Usuario, documentos: List<Documento>, bandeja: EstadoDocumento,
    onBandeja: (EstadoDocumento) -> Unit, onAbrir: (Long) -> Unit, onSalir: () -> Unit) {
    val titulo = when (bandeja) {
        EstadoDocumento.PENDIENTE -> "Documentos por revisar"
        EstadoDocumento.APROBADO -> "Documentos que aprobé"
        EstadoDocumento.RECHAZADO -> "Documentos que rechacé"
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Encabezado(usuario = usuario, onSalir = onSalir) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(titulo, style = MaterialTheme.typography.headlineMedium)
                Text("${usuario.nombre} · ${usuario.centro}", color = GrisTexto)
            }
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EstadoDocumento.entries.forEach { estado ->
                    FilterChip(selected = estado == bandeja, onClick = { onBandeja(estado) }, shape = CircleShape,
                        label = { Text(when (estado) {
                            EstadoDocumento.PENDIENTE -> "Pendientes"
                            EstadoDocumento.APROBADO -> "Aprobados"
                            EstadoDocumento.RECHAZADO -> "Rechazados"
                        }) })
                }
            }
        }
        item { AvisoInformativo(if (bandeja == EstadoDocumento.PENDIENTE) "Revisa los antecedentes de tu centro"
            else "Consulta tus decisiones y las observaciones de cada documento", Icons.AutoMirrored.Outlined.FactCheck) }
        item { Text("${documentos.size} documentos", color = GrisTexto) }
        if (documentos.isEmpty()) item {
            when (bandeja) {
                EstadoDocumento.PENDIENTE -> SinDocumentos("No hay documentos por revisar", "Los nuevos antecedentes pendientes aparecerán aquí.")
                EstadoDocumento.APROBADO -> SinDocumentos("Aún no has aprobado documentos", "Los documentos que apruebes aparecerán en esta sección.")
                EstadoDocumento.RECHAZADO -> SinDocumentos("Aún no has rechazado documentos", "Los documentos que rechaces aparecerán aquí junto con su detalle.")
            }
        }
        items(documentos, key = { it.id }) { TarjetaDocumento(it, mostrarTitular = true, onAbrir = onAbrir) }
        item { Text("Selecciona un documento para revisar su información.", color = GrisTexto) }
        item { PieDemostracion() }
    }
}
