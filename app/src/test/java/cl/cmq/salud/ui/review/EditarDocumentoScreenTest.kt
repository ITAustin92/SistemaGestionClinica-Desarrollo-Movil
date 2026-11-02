package cl.cmq.salud.ui.review

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cl.cmq.salud.repository.DemoDocumentoRepository
import cl.cmq.salud.ui.documentform.RegistroDocumentoScreen
import cl.cmq.salud.viewmodel.FormularioUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EditarDocumentoScreenTest {
    @get:Rule val compose = createComposeRule()
    private var salidas = 0

    private fun mostrar() {
        val original = DemoDocumentoRepository().documentos.value.first { it.id == 1L }
        compose.setContent {
            var form by remember { mutableStateOf(FormularioUiState(titulo = original.titulo,
                tipo = original.tipo, observacion = original.observacion,
                documentoOriginal = original, adjunto = original.adjunto)) }
            MaterialTheme {
                RegistroDocumentoScreen(form, { form = form.copy(titulo = it) },
                    { form = form.copy(tipo = it) }, { form = form.copy(observacion = it) },
                    {}, {}, {}, {}, {}, { salidas++ })
            }
        }
    }

    @Test fun edicionMuestraDatosPeroNoPermiteSustituirElAdjunto() {
        mostrar()
        compose.onNodeWithText("Editar documento").assertExists()
        compose.onNodeWithText("Certificado de capacitación").assertExists()
        compose.onNodeWithText("Elegir archivo").assertDoesNotExist()
        compose.onNodeWithText("Escanear").assertDoesNotExist()
        compose.onNodeWithText("Cambiar").assertDoesNotExist()
        compose.onNodeWithText("Generar PDF de ejemplo al guardar").assertDoesNotExist()
        compose.onNodeWithText("Guardar cambios").performScrollTo().assertExists()
        compose.onNodeWithContentDescription("Volver").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(1, salidas) }
    }

    @Test fun volverConCambiosPideConfirmacionYPermiteSeguirEditando() {
        mostrar()
        compose.onAllNodes(hasSetTextAction())[0].performTextReplacement("Nuevo título")
        compose.onNodeWithContentDescription("Volver").performScrollTo().performClick()
        compose.onNodeWithText("¿Descartar los cambios?").assertExists()
        compose.onNodeWithText("Seguir editando").performClick()
        compose.runOnIdle { assertEquals(0, salidas) }
        compose.onNodeWithText("Nuevo título").assertExists()
        compose.onNodeWithContentDescription("Volver").performScrollTo().performClick()
        compose.onNodeWithText("Descartar").performClick()
        compose.runOnIdle { assertEquals(1, salidas) }
    }
}
