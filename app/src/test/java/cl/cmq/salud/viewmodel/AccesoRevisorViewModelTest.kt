package cl.cmq.salud.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import cl.cmq.salud.model.Rol
import cl.cmq.salud.repository.ArchivoAdjuntoStore
import cl.cmq.salud.repository.DemoDocumentoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AccesoRevisorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private lateinit var viewModel: CmqViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        viewModel = CmqViewModel(DemoDocumentoRepository(), ArchivoAdjuntoStore(RuntimeEnvironment.getApplication()), dispatcher)
        store.put("acceso", viewModel)
    }

    @After fun terminar() {
        store.clear()
        Dispatchers.resetMain()
    }

    @Test fun revisorNoEntraConElAccesoNormal() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        assertNull(viewModel.entrar())
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
        assertNotNull(viewModel.uiState.value.errorAcceso)
    }

    @Test fun funcionarioEntraSinBiometria() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(1)
        assertNull(viewModel.iniciarAutenticacionRevisor())
        assertEquals(Rol.FUNCIONARIO, viewModel.entrar()?.rol)
        advanceUntilIdle()
        assertEquals(1L, viewModel.uiState.value.usuario?.id)
    }

    @Test fun soloElResultadoDelIntentoActivoAbreLaSesionDelDoctor() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        val solicitud = requireNotNull(viewModel.iniciarAutenticacionRevisor())
        assertNull(viewModel.iniciarAutenticacionRevisor())
        assertNull(viewModel.confirmarAutenticacionRevisor(solicitud + 1))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
        assertEquals("Dr. Pedro Soto", viewModel.confirmarAutenticacionRevisor(solicitud)?.nombre)
        assertNull(viewModel.confirmarAutenticacionRevisor(solicitud))
        advanceUntilIdle()
        assertEquals(Rol.REVISOR, viewModel.uiState.value.usuario?.rol)
        assertFalse(viewModel.uiState.value.autenticando)
    }

    @Test fun huellaNoReconocidaNoAbreSesionYPermiteReintentar() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        val solicitud = requireNotNull(viewModel.iniciarAutenticacionRevisor())
        viewModel.falloIntentoBiometrico(solicitud)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
        assertTrue(viewModel.uiState.value.autenticando)
        assertNotNull(viewModel.uiState.value.errorAcceso)
        assertNotNull(viewModel.confirmarAutenticacionRevisor(solicitud))
    }

    @Test fun cancelarImpideQueUnResultadoTardioAbraLaSesion() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        val solicitud = requireNotNull(viewModel.iniciarAutenticacionRevisor())
        viewModel.cancelarAutenticacionRevisor(solicitud, "Verificación cancelada")
        assertNull(viewModel.confirmarAutenticacionRevisor(solicitud))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
        assertFalse(viewModel.uiState.value.autenticando)
    }

    @Test fun resultadoDeUnIntentoAnteriorNoConfirmaUnoNuevo() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        val anterior = requireNotNull(viewModel.iniciarAutenticacionRevisor())
        viewModel.cancelarAutenticacionRevisor(anterior, "Cancelado")
        val nuevo = requireNotNull(viewModel.iniciarAutenticacionRevisor())
        assertNotEquals(anterior, nuevo)
        assertNull(viewModel.confirmarAutenticacionRevisor(anterior))
        viewModel.cancelarAutenticacionRevisor(anterior, "Error tardío")
        assertNotNull(viewModel.confirmarAutenticacionRevisor(nuevo))
    }

    @Test fun cambiarDePerfilInvalidaLaSolicitudAnterior() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        val solicitud = requireNotNull(viewModel.iniciarAutenticacionRevisor())
        viewModel.seleccionarUsuario(2)
        assertNull(viewModel.confirmarAutenticacionRevisor(solicitud))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
        assertEquals(2L, viewModel.entrar()?.id)
    }

    @Test fun sinBiometriaElRevisorSigueBloqueadoYSeOfreceConfigurar() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        viewModel.errorAcceso("Registra una huella", configurar = true)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
        assertTrue(viewModel.uiState.value.puedeConfigurarBiometria)
        assertNull(viewModel.confirmarAutenticacionRevisor(1))
        viewModel.seleccionarUsuario(1)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.puedeConfigurarBiometria)
        assertNull(viewModel.uiState.value.errorAcceso)
    }

    @Test fun cerrarSesionObligaAVerificarDeNuevo() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        val solicitud = requireNotNull(viewModel.iniciarAutenticacionRevisor())
        viewModel.confirmarAutenticacionRevisor(solicitud)
        viewModel.salir()
        viewModel.seleccionarUsuario(4)
        assertNull(viewModel.entrar())
        assertNull(viewModel.confirmarAutenticacionRevisor(solicitud))
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
    }

    @Test fun pulsacionSoloAbreElPerfilDelRevisorYNoPersisteElAcceso() = runTest(dispatcher) {
        advanceUntilIdle()
        assertNull(viewModel.entrarRevisorPorPulsacion())
        viewModel.seleccionarUsuario(1)
        assertNull(viewModel.entrarRevisorPorPulsacion())
        viewModel.seleccionarUsuario(4)
        assertEquals("Dr. Pedro Soto", viewModel.entrarRevisorPorPulsacion()?.nombre)
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.autenticando)
        viewModel.salir()
        viewModel.seleccionarUsuario(4)
        assertNull(viewModel.entrar())
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
    }

    @Test fun pulsacionNoInterrumpeUnaVerificacionBiometricaActiva() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        val solicitud = requireNotNull(viewModel.iniciarAutenticacionRevisor())
        assertNull(viewModel.entrarRevisorPorPulsacion())
        assertEquals(solicitud, viewModel.solicitudBiometricaPendiente)
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
    }
}
