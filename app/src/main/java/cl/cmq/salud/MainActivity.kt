package cl.cmq.salud

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import cl.cmq.salud.ui.CmqApp
import cl.cmq.salud.ui.theme.CmqTheme
import cl.cmq.salud.viewmodel.CmqViewModel
import cl.cmq.salud.ui.access.AutenticacionRevisor

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as CmqApplication).documentoRepository
        val vm = ViewModelProvider(this, CmqViewModel.factory(repository, (application as CmqApplication).archivos))[CmqViewModel::class.java]
        val autenticacion = AutenticacionRevisor(this, vm)
        setContent {
            CmqTheme {
                CmqApp(vm, onAutenticarRevisor = autenticacion::solicitar, onConfigurarBiometria = autenticacion::configurar)
            }
        }
    }
}
