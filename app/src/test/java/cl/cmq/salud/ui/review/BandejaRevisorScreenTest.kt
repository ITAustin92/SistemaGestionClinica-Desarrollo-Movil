package cl.cmq.salud.ui.review

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cl.cmq.salud.model.*
import cl.cmq.salud.repository.DemoDocumentoRepository
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class BandejaRevisorScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun lasPestanasPermitenConsultarYAbrirLosDocumentosRevisados() {
        val repository = DemoDocumentoRepository()
        val doctor = repository.usuarios.last()
        repository.revisar(doctor, 3, EstadoDocumento.RECHAZADO, "Falta firma")
        var abierto: Long? = null
        compose.setContent {
            var bandeja by remember { mutableStateOf(EstadoDocumento.PENDIENTE) }
            MaterialTheme {
                PendientesScreen(doctor, repository.documentosVisibles(doctor).filter {
                    it.estado == bandeja && (bandeja == EstadoDocumento.PENDIENTE || it.revision?.revisor == doctor.nombre)
                }, bandeja, onBandeja = { bandeja = it }, onAbrir = { abierto = it }, onSalir = {})
            }
        }
        compose.onNodeWithText("Aprobados").performClick()
        compose.onNodeWithText("Documentos que aprobé").assertExists()
        compose.onNodeWithText("Contrato de trabajo").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2L, abierto) }
        compose.onNodeWithText("Rechazados").performScrollTo().performClick()
        compose.onNodeWithText("Documentos que rechacé").assertExists()
        compose.onNodeWithText("Antecedente administrativo").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(3L, abierto) }
    }
}
