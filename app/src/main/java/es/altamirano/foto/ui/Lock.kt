package es.altamirano.foto.ui

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/** Huella (o cara) y, si no, el PIN/patrón del móvil. */
object Lock {
    private const val AUTH = BIOMETRIC_STRONG or DEVICE_CREDENTIAL

    fun available(a: FragmentActivity) =
        BiometricManager.from(a).canAuthenticate(AUTH) == BiometricManager.BIOMETRIC_SUCCESS

    fun ask(a: FragmentActivity, title: String, onOk: () -> Unit, onFail: () -> Unit = {}) {
        if (!available(a)) { onOk(); return }
        val prompt = BiometricPrompt(a, ContextCompat.getMainExecutor(a), object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) = onOk()
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) = onFail()
        })
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setAllowedAuthenticators(AUTH)
                .setConfirmationRequired(false)
                .build()
        )
    }
}
