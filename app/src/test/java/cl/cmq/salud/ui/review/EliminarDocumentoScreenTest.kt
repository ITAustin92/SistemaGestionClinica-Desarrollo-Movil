package cl.cmq.salud.ui.review

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cl.cmq.salud.repository.DemoDocumentoRepository
import cl.cmq.salud.ui.documentdetail.DetalleDocumentoScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class EliminarDocumentoScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun cancelarConservaElDocumentoYConfirmarEmiteUnaEliminacion() {
        val repo = DemoDocumentoRepository()
        var eliminaciones = 0
        compose.setContent {
            MaterialTheme {
                DetalleDocumentoScreen(repo.buscar(repo.usuarios.first(), 1), repo.usuarios.first(),
                    "", null, {}, {}, {}, {}, {}, {}, onEliminar = { eliminaciones++ })
            }
        }
        compose.onNodeWithText("Eliminar documento").performScrollTo().performClick()
        compose.onNodeWithText("Cancelar").performClick()
        compose.runOnIdle { assertEquals(0, eliminaciones) }
        compose.onNodeWithText("Eliminar documento").performScrollTo().performClick()
        compose.onNodeWithText("Eliminar", substring = false).performClick()
        compose.runOnIdle { assertEquals(1, eliminaciones) }
    }

    @Test fun documentoAprobadoNoOfreceEliminacion() {
        val repo = DemoDocumentoRepository()
        compose.setContent {
            MaterialTheme {
                DetalleDocumentoScreen(repo.buscar(repo.usuarios.first(), 2), repo.usuarios.first(),
                    "", null, {}, {}, {}, {}, {}, {})
            }
        }
        compose.onNodeWithText("Eliminar documento").assertDoesNotExist()
    }
}
