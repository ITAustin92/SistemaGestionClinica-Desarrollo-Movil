package cl.cmq.salud.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import cl.cmq.salud.model.*
import cl.cmq.salud.ui.access.AccesoScreen
import cl.cmq.salud.ui.documents.DocumentosScreen
import cl.cmq.salud.ui.documentform.RegistroDocumentoScreen
import cl.cmq.salud.ui.documentdetail.DetalleDocumentoScreen
import cl.cmq.salud.ui.review.PendientesScreen
import cl.cmq.salud.viewmodel.CmqViewModel

// La navegación pertenece a la UI: ViewModel y Repository no conocen NavController.
private object Rutas {
    const val ACCESO = "acceso"
    const val DOCUMENTOS = "documentos"
    const val REGISTRO = "registro"
    const val PENDIENTES = "pendientes"
    const val DETALLE = "detalle/{documentoId}"
    fun detalle(id: Long) = "detalle/$id"
}

@Composable
fun CmqApp(viewModel: CmqViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val salir: () -> Unit = {
        viewModel.salir()
        nav.navigate(Rutas.ACCESO) { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
    }
    val abrir: (Long) -> Unit = { id -> viewModel.abrirDetalle(); nav.navigate(Rutas.detalle(id)) }
    LaunchedEffect(state.mensaje) {
        state.mensaje?.let { snackbar.showSnackbar(it); viewModel.consumirMensaje() }
    }
    // Si Android restaura una ruta tras terminar el proceso, debe volver a elegir perfil.
    LaunchedEffect(state.usuario) {
        if (state.usuario == null && nav.currentDestination?.route != Rutas.ACCESO) {
            nav.navigate(Rutas.ACCESO) { popUpTo(nav.graph.id) { inclusive = true }; launchSingleTop = true }
        }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }, contentWindowInsets = WindowInsets.safeDrawing) { padding ->
        NavHost(navController = nav, startDestination = Rutas.ACCESO, modifier = Modifier.fillMaxSize().padding(padding)) {
            composable(Rutas.ACCESO) {
                AccesoScreen(viewModel.usuarios, viewModel.usuarios.find { it.id == state.usuarioSeleccionadoId },
                    viewModel::seleccionarUsuario, onEntrar = {
                        viewModel.entrar()?.let { usuario ->
                            nav.navigate(if (usuario.rol == Rol.REVISOR) Rutas.PENDIENTES else Rutas.DOCUMENTOS) {
                                popUpTo(Rutas.ACCESO) { inclusive = true }; launchSingleTop = true
                            }
                        }
                    })
            }
            composable(Rutas.DOCUMENTOS) {
                state.usuario?.let { usuario ->
                    DocumentosScreen(usuario, viewModel.visibles(), state.busqueda, state.filtroTipo, state.filtroEstado,
                        viewModel::buscar, viewModel::filtrarTipo, viewModel::filtrarEstado, viewModel::limpiarFiltros,
                        onNuevo = { viewModel.nuevoDocumento(); nav.navigate(Rutas.REGISTRO) }, onAbrir = abrir, onSalir = salir)
                }
            }
            composable(Rutas.PENDIENTES) {
                state.usuario?.let { PendientesScreen(it, viewModel.visibles(), onAbrir = abrir, onSalir = salir) }
            }
            composable(Rutas.REGISTRO) {
                if (state.usuario?.rol == Rol.FUNCIONARIO) {
                    RegistroDocumentoScreen(state.formulario, viewModel::cambiarTitulo, viewModel::cambiarTipo,
                        viewModel::cambiarObservacion, viewModel::adjuntarDemo,
                        onCamara = { viewModel.mostrarMensaje("La cámara se incorporará en la siguiente etapa. Por ahora puedes usar el adjunto de ejemplo.") },
                        onGuardar = {
                            viewModel.guardar()?.let { id ->
                                viewModel.abrirDetalle()
                                nav.navigate(Rutas.detalle(id)) { popUpTo(Rutas.REGISTRO) { inclusive = true } }
                            }
                        }, onVolver = { nav.popBackStack() })
                }
            }
            composable(Rutas.DETALLE, arguments = listOf(navArgument("documentoId") { type = NavType.LongType })) { entry ->
                val id = entry.arguments?.getLong("documentoId") ?: -1L
                state.usuario?.let { usuario ->
                    DetalleDocumentoScreen(viewModel.detalle(id), usuario, state.observacionRevision, state.errorRevision,
                        viewModel::cambiarRevision,
                        onAprobar = { viewModel.revisar(id, EstadoDocumento.APROBADO) },
                        onRechazar = { viewModel.revisar(id, EstadoDocumento.RECHAZADO) },
                        onAbrirAdjunto = { viewModel.mostrarMensaje("Este documento es un ejemplo sin archivo real. El visor se incorporará en la siguiente etapa.") },
                        onVolver = { nav.popBackStack() }, onSalir = salir)
                }
            }
        }
    }
}
