package cl.cmq.salud.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import cl.cmq.salud.model.*
import cl.cmq.salud.ui.access.AccesoScreen
import cl.cmq.salud.ui.components.*
import cl.cmq.salud.ui.documents.DocumentosScreen
import cl.cmq.salud.ui.documentform.RegistroDocumentoScreen
import cl.cmq.salud.ui.documentdetail.DetalleDocumentoScreen
import cl.cmq.salud.ui.review.PendientesScreen
import cl.cmq.salud.ui.profile.PerfilScreen
import cl.cmq.salud.viewmodel.CmqViewModel
import java.io.File

private object Rutas {
    const val ACCESO = "acceso"
    const val DOCUMENTOS = "documentos"
    const val REGISTRO = "registro"
    const val PENDIENTES = "pendientes"
    const val PERFIL = "perfil"
    const val DETALLE = "detalle/{documentoId}"
    fun detalle(id: Long) = "detalle/$id"
}

@Composable
fun CmqApp(viewModel: CmqViewModel, onAutenticarRevisor: () -> Unit, onConfigurarBiometria: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    var capturaPendiente by rememberSaveable { mutableStateOf<String?>(null) }
    var vistaPrevia by remember { mutableStateOf<Adjunto?>(null) }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { correcta ->
        capturaPendiente?.let { viewModel.recibirCaptura(it, correcta) }
        capturaPendiente = null
    }
    val selector = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let(viewModel::importar)
    }
    val ver: (Adjunto?) -> Unit = { adjunto ->
        if (adjunto?.rutaLocal == null) viewModel.mostrarMensaje("Este registro es un ejemplo sin archivo real.")
        else vistaPrevia = adjunto
    }
    val salir: () -> Unit = {
        vistaPrevia = null
        capturaPendiente?.let(viewModel::cancelarCaptura)
        capturaPendiente = null
        viewModel.salir()
        nav.navigate(Rutas.ACCESO) { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
    }
    val abrir: (Long) -> Unit = { id -> viewModel.abrirDetalle(); nav.navigate(Rutas.detalle(id)) }
    LaunchedEffect(state.mensaje) {
        state.mensaje?.let { snackbar.showSnackbar(it); viewModel.consumirMensaje() }
    }
    // Se conserva la información local, pero no se restaura una sesión institucional ficticia.
    LaunchedEffect(state.usuario) {
        if (state.usuario == null && nav.currentDestination?.route != Rutas.ACCESO) {
            vistaPrevia = null
            nav.navigate(Rutas.ACCESO) { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
        } else if (state.usuario != null && nav.currentDestination?.route == Rutas.ACCESO) {
            nav.navigate(if (state.usuario?.rol == Rol.REVISOR) Rutas.PENDIENTES else Rutas.DOCUMENTOS) {
                popUpTo(Rutas.ACCESO) { inclusive = true }; launchSingleTop = true
            }
        }
    }
    val acciones = state.usuario?.let { usuario ->
        AccionesPerfil(usuario, abrir = { nav.navigate(Rutas.PERFIL) { launchSingleTop = true } }, salir = salir)
    }
    CompositionLocalProvider(LocalAccionesPerfil provides acciones) {
        Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets.safeDrawing) { padding ->
            // El Scaffold ya reserva safeDrawing (incluye IME). Sus hijos no deben aplicarlo otra vez.
            NavHost(navController = nav, startDestination = Rutas.ACCESO,
                modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
                composable(Rutas.ACCESO) {
                    AccesoScreen(viewModel.usuarios, viewModel.usuarios.find { it.id == state.usuarioSeleccionadoId },
                        viewModel::seleccionarUsuario, onEntrar = {
                            if (viewModel.usuarios.find { it.id == state.usuarioSeleccionadoId }?.rol == Rol.REVISOR) onAutenticarRevisor()
                            else viewModel.entrar()
                        }, autenticando = state.autenticando, errorAcceso = state.errorAcceso,
                        puedeConfigurarBiometria = state.puedeConfigurarBiometria, onConfigurarBiometria = onConfigurarBiometria,
                        onMantenerUsuario = { viewModel.entrarRevisorPorPulsacion() })
                }
                composable(Rutas.DOCUMENTOS) {
                    state.usuario?.let { usuario ->
                        DocumentosScreen(usuario, viewModel.visibles(), state.busqueda, state.filtroTipo, state.filtroEstado,
                            viewModel::buscar, viewModel::filtrarTipo, viewModel::filtrarEstado, viewModel::limpiarFiltros,
                            onNuevo = { viewModel.nuevoDocumento(); nav.navigate(Rutas.REGISTRO) }, onAbrir = abrir, onSalir = salir)
                    }
                }
                composable(Rutas.PENDIENTES) {
                    state.usuario?.let { PendientesScreen(it, viewModel.visibles(), state.bandejaRevision,
                        viewModel::seleccionarBandejaRevision, onAbrir = abrir, onSalir = salir) }
                }
                composable(Rutas.PERFIL) {
                    state.usuario?.let { usuario ->
                        val cantidad = state.registros.count { documento ->
                            if (usuario.rol == Rol.FUNCIONARIO) documento.titularId == usuario.id else documento.centro == usuario.centro
                        }
                        PerfilScreen(usuario, cantidad, onVolver = { nav.popBackStack() }, onReiniciar = viewModel::reiniciar)
                    }
                }
                composable(Rutas.REGISTRO) {
                    if (state.usuario?.rol == Rol.FUNCIONARIO) {
                        RegistroDocumentoScreen(state.formulario, viewModel::cambiarTitulo, viewModel::cambiarTipo,
                            viewModel::cambiarObservacion, viewModel::adjuntarDemo,
                            onAdjuntar = {
                                try { selector.launch(arrayOf("application/pdf", "image/jpeg", "image/png")) }
                                catch (_: Exception) { viewModel.mostrarMensaje("No hay un selector de archivos disponible.") }
                            },
                            onCamara = {
                                viewModel.prepararCaptura { ruta ->
                                    capturaPendiente = ruta
                                    try {
                                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.archivos", File(ruta))
                                        camara.launch(uri)
                                    } catch (_: Exception) {
                                        viewModel.cancelarCaptura(ruta)
                                        capturaPendiente = null
                                        viewModel.mostrarMensaje("No se pudo abrir la cámara. Comprueba que haya una aplicación de cámara disponible.")
                                    }
                                }
                            }, onVistaPrevia = { ver(state.formulario.adjunto) },
                            onGuardar = {
                                viewModel.guardar { id ->
                                    viewModel.abrirDetalle()
                                    nav.navigate(Rutas.detalle(id)) { popUpTo(Rutas.REGISTRO) { inclusive = true } }
                                }
                            }, onVolver = { viewModel.descartarFormulario(); nav.popBackStack() })
                    }
                }
                composable(Rutas.DETALLE, arguments = listOf(navArgument("documentoId") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments?.getLong("documentoId") ?: -1L
                    state.usuario?.let { usuario ->
                        DetalleDocumentoScreen(viewModel.detalle(id), usuario, state.observacionRevision, state.errorRevision,
                            viewModel::cambiarRevision,
                            onAprobar = { viewModel.revisar(id, EstadoDocumento.APROBADO) },
                            onRechazar = { viewModel.revisar(id, EstadoDocumento.RECHAZADO) },
                            onAbrirAdjunto = { ver(viewModel.detalle(id)?.adjunto) },
                            onVolver = { nav.popBackStack() }, onSalir = salir)
                    }
                }
            }
        }
    }
    vistaPrevia?.let { VisorAdjunto(it, onCerrar = { vistaPrevia = null }, onError = viewModel::mostrarMensaje) }
    if (state.cargando || state.ocupado) Dialog(onDismissRequest = {}) {
        Surface(shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                CircularProgressIndicator()
                Text(if (state.cargando) "Cargando documentos…" else "Procesando…")
            }
        }
    }
    if (state.errorCarga != null) AlertDialog(onDismissRequest = {}, title = { Text("Almacenamiento no disponible") },
        text = { Text(requireNotNull(state.errorCarga)) },
        confirmButton = { TextButton(onClick = viewModel::cargar) { Text("Reintentar") } })
}
