package cl.cmq.salud.ui.access

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import cl.cmq.salud.viewmodel.CmqViewModel

/** Android verifica la biometría; la app recibe únicamente el resultado, nunca la huella. */
class AutenticacionRevisor(private val activity: FragmentActivity, private val viewModel: CmqViewModel) {
    // La Activity reconstruye el callback al rotar y lo reconecta al intento del ViewModel.
    private var solicitudActiva = viewModel.solicitudBiometricaPendiente
    private val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                solicitudActiva?.let(viewModel::confirmarAutenticacionRevisor)
                solicitudActiva = null
            }

            override fun onAuthenticationFailed() {
                solicitudActiva?.let(viewModel::falloIntentoBiometrico)
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                val mensaje = when (errorCode) {
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON, BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_CANCELED -> "Verificación cancelada. No se abrió la sesión del revisor."
                    BiometricPrompt.ERROR_LOCKOUT, BiometricPrompt.ERROR_LOCKOUT_PERMANENT ->
                        "La biometría está bloqueada tras varios intentos. Sigue las indicaciones de Android y vuelve a intentarlo."
                    else -> "No se pudo verificar tu identidad: $errString"
                }
                solicitudActiva?.let { viewModel.cancelarAutenticacionRevisor(it, mensaje) }
                solicitudActiva = null
            }
        })

    fun solicitar() {
        if (viewModel.solicitudBiometricaPendiente != null) return
        val estado = try { BiometricManager.from(activity).canAuthenticate(BIOMETRIC_STRONG) }
        catch (_: RuntimeException) {
            viewModel.errorAcceso("No se pudo consultar el sensor biométrico. Vuelve a intentarlo.")
            return
        }
        when (estado) {
            BiometricManager.BIOMETRIC_SUCCESS -> Unit
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                viewModel.errorAcceso("Registra una huella o biometría fuerte en los ajustes del dispositivo para entrar como revisor.", configurar = true)
                return
            }
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                viewModel.errorAcceso("Este dispositivo no dispone de biometría fuerte compatible. El acceso al revisor permanece bloqueado.")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                viewModel.errorAcceso("El sensor biométrico no está disponible en este momento. Inténtalo de nuevo.")
                return
            }
            BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> {
                viewModel.errorAcceso("Android requiere una actualización de seguridad antes de utilizar la biometría.")
                return
            }
            else -> {
                viewModel.errorAcceso("No se puede utilizar la biometría fuerte en este dispositivo. Comprueba su configuración.")
                return
            }
        }
        val solicitud = viewModel.iniciarAutenticacionRevisor() ?: return
        solicitudActiva = solicitud
        try {
            prompt.authenticate(BiometricPrompt.PromptInfo.Builder()
                .setTitle("Acceso al revisor")
                .setSubtitle("Dr. Pedro Soto · Centro Médico Quilpué")
                .setDescription("Verifica tu identidad con la biometría registrada en este dispositivo.")
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .setNegativeButtonText("Cancelar")
                .build())
        } catch (_: RuntimeException) {
            viewModel.cancelarAutenticacionRevisor(solicitud, "No se pudo iniciar la verificación biométrica. Vuelve a intentarlo.")
            solicitudActiva = null
        }
    }

    fun configurar() {
        try {
            val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent(Settings.ACTION_BIOMETRIC_ENROLL).putExtra(Settings.EXTRA_BIOMETRIC_AUTHENTICATORS_ALLOWED, BIOMETRIC_STRONG)
            } else Intent(Settings.ACTION_SECURITY_SETTINGS)
            activity.startActivity(intent)
        } catch (_: RuntimeException) {
            viewModel.errorAcceso("Abre los ajustes de seguridad del dispositivo y registra una huella para continuar.")
        }
    }
}
