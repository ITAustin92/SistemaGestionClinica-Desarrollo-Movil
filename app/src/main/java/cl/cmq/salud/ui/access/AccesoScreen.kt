package cl.cmq.salud.ui.access

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cl.cmq.salud.model.Usuario
import cl.cmq.salud.ui.components.*
import cl.cmq.salud.ui.theme.GrisTexto

@Composable
fun AccesoScreen(usuarios: List<Usuario>, seleccionado: Usuario?, onSeleccionar: (Long) -> Unit, onEntrar: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)) {
        Encabezado()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Bienvenido", style = MaterialTheme.typography.headlineLarge)
            Text("Selecciona un perfil para continuar", color = GrisTexto, style = MaterialTheme.typography.bodyLarge)
        }
        ElevatedCard(colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
            elevation = CardDefaults.elevatedCardElevation(3.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                IconoCircular(Icons.Outlined.PersonOutline, Modifier.size(78.dp))
                Text("Acceso de demostración", style = MaterialTheme.typography.titleLarge)
                SelectorOpciones("Usuario", seleccionado?.nombre ?: "Selecciona un usuario", usuarios, { it.nombre }, { onSeleccionar(it.id) })
                if (seleccionado != null) {
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Perfil: ${seleccionado.rol.etiqueta}", color = GrisTexto)
                        Text(seleccionado.centro, color = GrisTexto)
                    }
                }
                BotonPrincipal("Entrar", onEntrar, habilitado = seleccionado != null)
            }
        }
        Text("Utiliza un usuario ficticio para explorar la aplicación.", color = GrisTexto, style = MaterialTheme.typography.bodyMedium)
        PieDemostracion()
    }
}
