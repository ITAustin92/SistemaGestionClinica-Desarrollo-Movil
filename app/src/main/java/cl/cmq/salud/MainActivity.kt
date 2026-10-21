package cl.cmq.salud

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.cmq.salud.ui.CmqApp
import cl.cmq.salud.ui.theme.CmqTheme
import cl.cmq.salud.viewmodel.CmqViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as CmqApplication).documentoRepository
        setContent {
            CmqTheme {
                val vm: CmqViewModel = viewModel(factory = CmqViewModel.factory(repository))
                CmqApp(vm)
            }
        }
    }
}
