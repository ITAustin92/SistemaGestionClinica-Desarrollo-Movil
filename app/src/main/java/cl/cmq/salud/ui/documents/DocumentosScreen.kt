package cl.cmq.salud.ui.documents

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.cmq.salud.model.*
import cl.cmq.salud.ui.components.*
import cl.cmq.salud.ui.theme.GrisTexto

@Composable
fun DocumentosScreen(usuario: Usuario, documentos: List<Documento>, busqueda: String,
    tipo: TipoDocumento?, estado: EstadoDocumento?, onBuscar: (String) -> Unit,
    onTipo: (TipoDocumento?) -> Unit, onEstado: (EstadoDocumento?) -> Unit,
    onLimpiar: () -> Unit, onNuevo: () -> Unit, onAbrir: (Long) -> Unit, onSalir: () -> Unit) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = 22.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Encabezado(usuario = usuario, onSalir = onSalir) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mis documentos", style = MaterialTheme.typography.headlineMedium)
                Text("${usuario.nombre} · ${usuario.centro}", color = GrisTexto)
            }
        }
        item { AvisoInformativo("Tu documentación, en un solo lugar") }
        item {
            OutlinedTextField(value = busqueda, onValueChange = onBuscar, placeholder = { Text("Buscar documento") },
                leadingIcon = { Icon(Icons.Outlined.Search, null) }, singleLine = true,
                modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SelectorOpciones("", "Tipo: ${tipo?.etiqueta ?: "Todos"}", listOf<TipoDocumento?>(null) + TipoDocumento.entries,
                    { it?.etiqueta ?: "Todos" }, onTipo, Modifier.weight(1f))
                SelectorOpciones("", "Estado: ${estado?.etiqueta ?: "Todos"}", listOf<EstadoDocumento?>(null) + EstadoDocumento.entries,
                    { it?.etiqueta ?: "Todos" }, onEstado, Modifier.weight(1f))
            }
        }
        item { Text("${documentos.size} documentos", color = GrisTexto) }
        if (documentos.isEmpty()) item {
            SinDocumentos("No hay documentos para mostrar", "Carga un documento o cambia los filtros de búsqueda.", onLimpiar)
        }
        items(documentos, key = { it.id }) { TarjetaDocumento(it, onAbrir = onAbrir) }
        item { BotonPrincipal("Nuevo documento", onNuevo, icono = Icons.Outlined.Add) }
        item { PieDemostracion() }
    }
}
