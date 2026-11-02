package cl.cmq.salud.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModelStore
import cl.cmq.salud.model.*
import cl.cmq.salud.repository.ArchivoAdjuntoStore
import cl.cmq.salud.repository.DemoDocumentoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
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
class BandejaRevisorViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private lateinit var viewModel: CmqViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        val ejemplos = DemoDocumentoRepository().documentos.value.map { doc ->
            when (doc.id) {
                3L -> doc.copy(estado = EstadoDocumento.RECHAZADO,
                    revision = RevisionDocumento("Dr. Pedro Soto", "Faltan antecedentes", doc.fechaCarga))
                4L -> doc.copy(estado = EstadoDocumento.APROBADO,
                    revision = RevisionDocumento("Otra persona revisora", "", doc.fechaCarga))
                5L -> doc.copy(estado = EstadoDocumento.APROBADO,
                    revision = RevisionDocumento("Dr. Pedro Soto", "", doc.fechaCarga))
                else -> doc
            }
        }
        viewModel = CmqViewModel(DemoDocumentoRepository(ejemplos), ArchivoAdjuntoStore(RuntimeEnvironment.getApplication()), dispatcher)
        store.put("bandeja", viewModel)
        dispatcher.scheduler.advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        viewModel.confirmarAutenticacionRevisor(requireNotNull(viewModel.iniciarAutenticacionRevisor()))
        dispatcher.scheduler.advanceUntilIdle()
    }

    @After fun terminar() { store.clear(); Dispatchers.resetMain() }

    private fun entrarFuncionario() {
        viewModel.salir(); viewModel.seleccionarUsuario(1); viewModel.entrar()
    }

    @Test fun editarPrecargaValidaYGuardaElMismoDocumentoConSuAdjunto() = runTest(dispatcher) {
        entrarFuncionario()
        val original = requireNotNull(viewModel.detalle(1))
        assertTrue(viewModel.editarDocumento(1))
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.formulario.tieneCambios)
        assertEquals(original.adjunto, viewModel.uiState.value.formulario.adjunto)
        viewModel.adjuntarDemo()
        assertEquals(original.adjunto, viewModel.uiState.value.formulario.adjunto)
        viewModel.cambiarTitulo(" ")
        var guardado: Long? = null
        viewModel.guardar { guardado = it }
        advanceUntilIdle()
        assertNull(guardado)
        assertTrue(viewModel.uiState.value.formulario.errores.containsKey("titulo"))
        viewModel.cambiarTitulo("Título corregido")
        viewModel.cambiarTipo(TipoDocumento.CONTRATO)
        viewModel.cambiarObservacion("Corrección de datos")
        viewModel.guardar { guardado = it }
        advanceUntilIdle()
        assertEquals(1L, guardado)
        assertEquals("Título corregido", viewModel.detalle(1)?.titulo)
        assertEquals(TipoDocumento.CONTRATO, viewModel.detalle(1)?.tipo)
        assertEquals("Corrección de datos", viewModel.detalle(1)?.observacion)
        assertEquals(original.adjunto, viewModel.detalle(1)?.adjunto)
        assertFalse(viewModel.uiState.value.formulario.editando)
    }

    @Test fun descartarEdicionNoModificaDocumentoYLosRevisadosNoSeEditan() = runTest(dispatcher) {
        entrarFuncionario()
        val original = viewModel.detalle(1)
        assertTrue(viewModel.editarDocumento(1))
        viewModel.cambiarTitulo("Cambio descartado")
        viewModel.descartarFormulario()
        advanceUntilIdle()
        assertEquals(original, viewModel.detalle(1))
        assertFalse(viewModel.editarDocumento(2))
        assertFalse(viewModel.editarDocumento(4))
    }

    @Test fun eliminarActualizaLaListaYNotificaSoloTrasConfirmarElRepositorio() = runTest(dispatcher) {
        viewModel.salir()
        viewModel.seleccionarUsuario(1)
        viewModel.entrar()
        var eliminado = false
        viewModel.eliminar(1) { eliminado = true }
        assertFalse(eliminado)
        advanceUntilIdle()
        assertTrue(eliminado)
        assertNull(viewModel.detalle(1))
        assertEquals("Documento eliminado del dispositivo.", viewModel.uiState.value.mensaje)
    }

    @Test fun unRevisorNoPuedeEliminarNiRecibeCallbackDeExito() = runTest(dispatcher) {
        var eliminado = false
        viewModel.eliminar(1) { eliminado = true }
        advanceUntilIdle()
        assertFalse(eliminado)
        assertNotNull(viewModel.detalle(1))
        assertFalse(viewModel.uiState.value.ocupado)
        assertNotNull(viewModel.uiState.value.mensaje)
    }

    @Test fun separaPendientesYAprobadosYRechazadosPropiosDelCentro() = runTest(dispatcher) {
        assertEquals(listOf(1L), viewModel.visibles().map { it.id })
        viewModel.seleccionarBandejaRevision(EstadoDocumento.APROBADO)
        assertEquals(listOf(2L), viewModel.visibles().map { it.id })
        viewModel.seleccionarBandejaRevision(EstadoDocumento.RECHAZADO)
        assertEquals(listOf(3L), viewModel.visibles().map { it.id })
        assertEquals("Faltan antecedentes", viewModel.detalle(3)?.revision?.observacion)
        assertNull(viewModel.detalle(5))
    }

    @Test fun aprobarMueveElDocumentoAPorMiSinPerderElDetalle() = runTest(dispatcher) {
        viewModel.revisar(1, EstadoDocumento.APROBADO)
        advanceUntilIdle()
        assertTrue(viewModel.visibles().isEmpty())
        viewModel.seleccionarBandejaRevision(EstadoDocumento.APROBADO)
        assertEquals(setOf(1L, 2L), viewModel.visibles().map { it.id }.toSet())
        assertEquals(EstadoDocumento.APROBADO, viewModel.detalle(1)?.estado)
        assertEquals("Dr. Pedro Soto", viewModel.detalle(1)?.revision?.revisor)
    }

    @Test fun rechazarMantieneLasObservacionesDisponiblesEnLaBandeja() = runTest(dispatcher) {
        viewModel.cambiarRevision("Falta la firma")
        viewModel.revisar(1, EstadoDocumento.RECHAZADO)
        advanceUntilIdle()
        assertTrue(viewModel.visibles().isEmpty())
        viewModel.seleccionarBandejaRevision(EstadoDocumento.RECHAZADO)
        assertEquals(setOf(1L, 3L), viewModel.visibles().map { it.id }.toSet())
        assertEquals("Falta la firma", viewModel.detalle(1)?.revision?.observacion)
        viewModel.seleccionarBandejaRevision(EstadoDocumento.APROBADO)
        assertEquals(listOf(2L), viewModel.visibles().map { it.id })
    }

    @Test fun cambiarDeSesionRestableceLaBandejaSinAlterarLasDecisiones() = runTest(dispatcher) {
        viewModel.seleccionarBandejaRevision(EstadoDocumento.RECHAZADO)
        viewModel.salir()
        viewModel.seleccionarUsuario(1)
        viewModel.entrar()
        advanceUntilIdle()
        assertEquals(EstadoDocumento.PENDIENTE, viewModel.uiState.value.bandejaRevision)
        assertEquals(setOf(1L, 2L, 3L), viewModel.visibles().map { it.id }.toSet())
        assertEquals(EstadoDocumento.RECHAZADO, viewModel.detalle(3)?.estado)
    }
}
