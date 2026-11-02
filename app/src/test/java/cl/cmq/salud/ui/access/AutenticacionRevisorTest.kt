package cl.cmq.salud.ui.access

import android.app.Application
import android.content.pm.PackageManager
import android.os.Looper
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelStore
import cl.cmq.salud.R
import cl.cmq.salud.repository.ArchivoAdjuntoStore
import cl.cmq.salud.repository.DemoDocumentoRepository
import cl.cmq.salud.viewmodel.CmqViewModel
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
import org.robolectric.Robolectric
import org.robolectric.shadow.api.Shadow
import org.robolectric.shadows.ShadowFingerprintManager
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.android.controller.ActivityController

@Suppress("DEPRECATION")
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class AutenticacionRevisorTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private lateinit var controller: ActivityController<FragmentActivity>
    private lateinit var activity: FragmentActivity
    private lateinit var viewModel: CmqViewModel

    @Before fun preparar() {
        Dispatchers.setMain(dispatcher)
        controller = Robolectric.buildActivity(FragmentActivity::class.java)
        activity = controller.get()
        activity.setTheme(R.style.Theme_CMQSalud)
        controller.setup()
        viewModel = CmqViewModel(DemoDocumentoRepository(), ArchivoAdjuntoStore(activity.applicationContext), dispatcher)
        store.put("acceso", viewModel)
    }

    @After fun terminar() {
        controller.pause().stop().destroy()
        store.clear()
        Dispatchers.resetMain()
    }

    private fun sensor(): ShadowFingerprintManager = Shadow.extract(requireNotNull(activity.getSystemService("fingerprint")))

    @Test fun dialogoDeAndroid8SoloAbreAlRecibirUnaHuellaValida() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        shadowOf(activity.packageManager).setSystemFeature(PackageManager.FEATURE_FINGERPRINT, true)
        val sensor = sensor()
        sensor.setIsHardwareDetected(true)
        sensor.setHasEnrolledFingerprints(true)
        AutenticacionRevisor(activity, viewModel).solicitar()
        shadowOf(Looper.getMainLooper()).idle()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.autenticando)
        assertNull(viewModel.uiState.value.usuario)
        sensor.authenticationFails()
        shadowOf(Looper.getMainLooper()).idle()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
        sensor.authenticationSucceeds()
        shadowOf(Looper.getMainLooper()).idle()
        advanceUntilIdle()
        assertEquals("Dr. Pedro Soto", viewModel.uiState.value.usuario?.nombre)
    }

    @Test fun noTenerHuellasRegistradasNoPermiteEntrar() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        shadowOf(activity.packageManager).setSystemFeature(PackageManager.FEATURE_FINGERPRINT, true)
        val sensor = sensor()
        sensor.setIsHardwareDetected(true)
        sensor.setHasEnrolledFingerprints(false)
        AutenticacionRevisor(activity, viewModel).solicitar()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
        assertFalse(viewModel.uiState.value.autenticando)
        assertTrue(viewModel.uiState.value.puedeConfigurarBiometria)
    }

    @Test fun noTenerSensorNoOfreceUnAccesoAlternativo() = runTest(dispatcher) {
        advanceUntilIdle()
        viewModel.seleccionarUsuario(4)
        shadowOf(activity.packageManager).setSystemFeature(PackageManager.FEATURE_FINGERPRINT, false)
        sensor().setIsHardwareDetected(false)
        AutenticacionRevisor(activity, viewModel).solicitar()
        advanceUntilIdle()
        assertNull(viewModel.uiState.value.usuario)
        assertNotNull(viewModel.uiState.value.errorAcceso)
        assertFalse(viewModel.uiState.value.puedeConfigurarBiometria)
    }
}
