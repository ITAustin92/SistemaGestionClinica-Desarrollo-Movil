package cl.cmq.salud.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.semantics.Role
import cl.cmq.salud.model.*
import cl.cmq.salud.ui.theme.*

@Composable
fun MarcaCmq(modifier: Modifier = Modifier, compacta: Boolean = false) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(Modifier.size(if (compacta) 35.dp else 43.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.Folder, null, tint = Turquesa, modifier = Modifier.fillMaxSize())
            Icon(Icons.Outlined.Check, null, tint = Turquesa, modifier = Modifier.size(19.dp).offset(y = 3.dp))
        }
        Column {
            Text("CMQ SALUD", color = Turquesa, style = if (compacta) MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleLarge)
            Text("Gestión documental", color = GrisTexto, style = MaterialTheme.typography.bodySmall)
        }
    }
}

data class AccionesPerfil(val usuario: Usuario, val abrir: () -> Unit, val salir: () -> Unit)
val LocalAccionesPerfil = staticCompositionLocalOf<AccionesPerfil?> { null }

@Composable
fun Encabezado(onVolver: (() -> Unit)? = null, usuario: Usuario? = null, onSalir: (() -> Unit)? = null) {
    val perfil = LocalAccionesPerfil.current
    var menu by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onVolver != null) IconButton(onClick = onVolver) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Volver") }
        MarcaCmq(Modifier.weight(1f), compacta = onVolver != null)
        if (perfil != null) {
            Box {
                IconButton(onClick = { menu = true }) {
                    Box(Modifier.size(38.dp).background(Aqua, CircleShape), contentAlignment = Alignment.Center) {
                        Text(perfil.usuario.nombre.removePrefix("Dr. ").split(' ').filter { it.isNotBlank() }
                            .take(2).joinToString("") { it.first().uppercase() }, color = Turquesa,
                            fontWeight = FontWeight.Bold, modifier = Modifier.semantics { contentDescription = "Abrir menú de perfil" })
                    }
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Mi perfil") }, leadingIcon = { Icon(Icons.Outlined.Person, null) },
                        onClick = { menu = false; perfil.abrir() })
                    DropdownMenuItem(text = { Text("Cerrar sesión") }, leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Logout, null) },
                        onClick = { menu = false; perfil.salir() })
                }
            }
        } else if (onSalir != null) {
            TextButton(onClick = onSalir) { Text("Salir") }
        }
    }
}

@Composable
fun BotonPrincipal(texto: String, onClick: () -> Unit, modifier: Modifier = Modifier, habilitado: Boolean = true, icono: ImageVector? = null) {
    Button(onClick = onClick, enabled = habilitado, shape = CircleShape,
        modifier = modifier.fillMaxWidth().heightIn(min = 52.dp), contentPadding = PaddingValues(16.dp)) {
        if (icono != null) { Icon(icono, null, Modifier.size(22.dp)); Spacer(Modifier.width(9.dp)) }
        Text(texto, textAlign = TextAlign.Center)
    }
}

@Composable
fun CampoFormulario(valor: String, etiqueta: String, onCambio: (String) -> Unit, modifier: Modifier = Modifier,
                    error: String? = null, ayuda: String? = null, multilinea: Boolean = false) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Text(etiqueta, style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(value = valor, onValueChange = onCambio, isError = error != null,
            modifier = Modifier.fillMaxWidth(), singleLine = !multilinea, minLines = if (multilinea) 3 else 1,
            shape = RoundedCornerShape(14.dp),
            supportingText = if (error != null || ayuda != null) { { Text(error ?: ayuda.orEmpty()) } } else null)
    }
}

@Composable
fun <T> SelectorOpciones(etiqueta: String, seleccion: String, opciones: List<T>, texto: (T) -> String,
                        onSeleccionar: (T) -> Unit, modifier: Modifier = Modifier, error: String? = null, habilitado: Boolean = true,
                        onPulsacionProlongada: (() -> Unit)? = null) {
    var expandido by remember { mutableStateOf(false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(7.dp)) {
        if (etiqueta.isNotEmpty()) Text(etiqueta, style = MaterialTheme.typography.titleMedium)
        Box {
            if (onPulsacionProlongada != null) {
                val configuracion = LocalViewConfiguration.current
                val pulsacion = remember(configuracion) {
                    object : ViewConfiguration by configuracion {
                        override val longPressTimeoutMillis: Long = 3_000L
                    }
                }
                CompositionLocalProvider(LocalViewConfiguration provides pulsacion) {
                    Surface(shape = RoundedCornerShape(14.dp), color = Color.Transparent,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp)
                            .combinedClickable(enabled = habilitado, role = Role.Button,
                                onClick = { expandido = true }, onLongClick = onPulsacionProlongada)
                            .padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(seleccion, modifier = Modifier.weight(1f), color = Grafito, textAlign = TextAlign.Start,
                                style = MaterialTheme.typography.bodyLarge)
                            Icon(Icons.Outlined.ExpandMore, null, tint = GrisTexto)
                        }
                    }
                }
            } else OutlinedButton(onClick = { expandido = true }, enabled = habilitado, shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                Text(seleccion, modifier = Modifier.weight(1f), color = Grafito, textAlign = TextAlign.Start,
                    style = MaterialTheme.typography.bodyLarge)
                Icon(Icons.Outlined.ExpandMore, null, tint = GrisTexto)
            }
            DropdownMenu(expanded = expandido, onDismissRequest = { expandido = false }) {
                opciones.forEach { opcion ->
                    DropdownMenuItem(text = { Text(texto(opcion)) }, onClick = { onSeleccionar(opcion); expandido = false })
                }
            }
        }
        if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun IconoCircular(icono: ImageVector, modifier: Modifier = Modifier) {
    Box(modifier.size(48.dp).background(Aqua, CircleShape), contentAlignment = Alignment.Center) {
        Icon(icono, null, tint = Turquesa, modifier = Modifier.size(26.dp))
    }
}

@Composable
fun EtiquetaEstado(estado: EstadoDocumento) {
    val (fondo, texto) = when (estado) {
        EstadoDocumento.PENDIENTE -> Color(0xFFFFE6B3) to Color(0xFF865500)
        EstadoDocumento.APROBADO -> Color(0xFFDEF2D5) to Color(0xFF216922)
        EstadoDocumento.RECHAZADO -> Color(0xFFFFE3E3) to Color(0xFF9E2731)
    }
    Text(estado.etiqueta, color = texto, style = MaterialTheme.typography.labelMedium,
        modifier = Modifier.background(fondo, CircleShape).padding(horizontal = 12.dp, vertical = 6.dp))
}

@Composable
fun TarjetaDocumento(documento: Documento, mostrarTitular: Boolean = false, onAbrir: (Long) -> Unit) {
    ElevatedCard(onClick = { onAbrir(documento.id) }, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color.White),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            IconoCircular(Icons.Outlined.Description)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(documento.titulo, style = MaterialTheme.typography.titleMedium)
                if (mostrarTitular) Text(documento.titular, color = GrisTexto, style = MaterialTheme.typography.bodyMedium)
                Text("${documento.tipo.etiqueta} · ${documento.fechaCarga.substringBefore(" · ")}", color = GrisTexto, style = MaterialTheme.typography.bodySmall)
                EtiquetaEstado(documento.estado)
            }
            Icon(Icons.Outlined.ChevronRight, null, tint = GrisTexto)
        }
    }
}

@Composable
fun AvisoInformativo(texto: String, icono: ImageVector = Icons.Outlined.Folder) {
    Row(Modifier.fillMaxWidth().background(Aqua, RoundedCornerShape(16.dp)).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icono, null, tint = Turquesa)
        Text(texto, color = Color(0xFF006A70), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun PieDemostracion() {
    Text("Datos de demostración", style = MaterialTheme.typography.bodySmall, color = GrisTexto,
        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp))
}

@Composable
fun SinDocumentos(titulo: String, descripcion: String, onLimpiar: (() -> Unit)? = null) {
    Column(Modifier.fillMaxWidth().padding(vertical = 36.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IconoCircular(Icons.Outlined.FolderOpen)
        Text(titulo, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(descripcion, color = GrisTexto, textAlign = TextAlign.Center)
        if (onLimpiar != null) TextButton(onClick = onLimpiar) { Text("Limpiar filtros") }
    }
}
