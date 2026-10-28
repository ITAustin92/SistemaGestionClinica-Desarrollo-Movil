package cl.cmq.salud.ui.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.cmq.salud.model.Usuario
import cl.cmq.salud.ui.components.*

@Composable
fun PerfilScreen(usuario: Usuario, cantidad: Int, onVolver: () -> Unit, onReiniciar: () -> Unit) {
    var confirmar by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Encabezado(onVolver = onVolver)
        Text("Mi perfil", style = MaterialTheme.typography.headlineMedium)
        ElevatedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(usuario.nombre, style = MaterialTheme.typography.titleLarge)
                Text("Perfil: ${usuario.rol.etiqueta}")
                Text("Centro: ${usuario.centro}")
                Text("$cantidad documentos en este dispositivo")
            }
        }
        AvisoInformativo("Perfil de demostración. Los documentos y las revisiones se conservan en SQLite al cerrar la aplicación. La sesión se selecciona nuevamente al abrirla.")
        Text("Demostración técnica", style = MaterialTheme.typography.titleLarge)
        Text("Permite recuperar el estado inicial para repetir la presentación del proyecto.")
        OutlinedButton(onClick = { confirmar = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
            Icon(Icons.Outlined.DeleteForever, null)
            Spacer(Modifier.width(8.dp))
            Text("Reiniciar datos locales")
        }
        PieDemostracion()
    }
    if (confirmar) AlertDialog(
        onDismissRequest = { confirmar = false },
        title = { Text("¿Reiniciar la demostración?") },
        text = { Text("Se eliminarán todos los documentos añadidos, sus fotos y archivos, las revisiones y el formulario sin guardar de todos los perfiles. Se restaurarán los cinco documentos de ejemplo y volverás al acceso. Esta acción no se puede deshacer.") },
        confirmButton = { TextButton(onClick = { confirmar = false; onReiniciar() }) { Text("Eliminar y reiniciar", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { confirmar = false }) { Text("Cancelar") } })
}
