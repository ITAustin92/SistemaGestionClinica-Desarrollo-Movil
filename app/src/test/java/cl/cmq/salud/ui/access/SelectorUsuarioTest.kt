package cl.cmq.salud.ui.access

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import cl.cmq.salud.ui.components.SelectorOpciones
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SelectorUsuarioTest {
    @get:Rule val compose = createComposeRule()

    private fun mostrar(onMantener: () -> Unit) {
        compose.setContent {
            MaterialTheme {
                SelectorOpciones("Usuario", "Dr. Pedro Soto", listOf("Dr. Pedro Soto", "Ángel Quintero"),
                    { it }, {}, onPulsacionProlongada = onMantener)
            }
        }
    }

    @Test fun tresSegundosActivanLaPulsacionUnaSolaVez() {
        var llamadas = 0
        mostrar { llamadas++ }
        compose.onNodeWithText("Dr. Pedro Soto").performTouchInput { longClick(durationMillis = 3_100L) }
        compose.runOnIdle { assertEquals(1, llamadas) }
        compose.onNodeWithText("Ángel Quintero").assertDoesNotExist()
    }

    @Test fun soltarAntesDeTresSegundosSoloAbreLasOpciones() {
        var llamadas = 0
        mostrar { llamadas++ }
        compose.onNodeWithText("Dr. Pedro Soto").performTouchInput {
            down(center)
            advanceEventTime(2_900L)
            up()
        }
        compose.runOnIdle { assertEquals(0, llamadas) }
        compose.onNodeWithText("Ángel Quintero").assertExists()
    }

    @Test fun pulsacionProlongadaNoSeActivaEnUnSelectorDeshabilitado() {
        var llamadas = 0
        compose.setContent {
            MaterialTheme {
                SelectorOpciones("Usuario", "Dr. Pedro Soto", listOf("Dr. Pedro Soto"), { it }, {},
                    habilitado = false, onPulsacionProlongada = { llamadas++ })
            }
        }
        compose.onNodeWithText("Dr. Pedro Soto").performTouchInput { longClick(durationMillis = 3_100L) }
        compose.runOnIdle { assertEquals(0, llamadas) }
    }
}
