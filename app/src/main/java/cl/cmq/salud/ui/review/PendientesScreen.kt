package cl.cmq.salud.ui.review

import androidx.compose.foundation.layout.*
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
import cl.cmq.salud.ui.components.*
import cl.cmq.salud.ui.theme.GrisTexto

@Composable
fun PendientesScreen(usuario: Usuario, documentos: List<Documento>, onAbrir: (Long) -> Unit, onSalir: () -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { Encabezado(usuario = usuario, onSalir = onSalir) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Documentos por revisar", style = MaterialTheme.typography.headlineMedium)
                Text("${usuario.nombre} · ${usuario.centro}", color = GrisTexto)
            }
        }
        item { AvisoInformativo("Revisa los antecedentes de tu centro", Icons.AutoMirrored.Outlined.FactCheck) }
        item { Text("${documentos.size} documentos pendientes", color = GrisTexto) }
        if (documentos.isEmpty()) item { SinDocumentos("No hay documentos por revisar", "Los nuevos antecedentes pendientes aparecerán aquí.") }
        items(documentos, key = { it.id }) { TarjetaDocumento(it, mostrarTitular = true, onAbrir = onAbrir) }
        item { Text("Selecciona un documento para revisar su información.", color = GrisTexto) }
        item { PieDemostracion() }
    }
}
