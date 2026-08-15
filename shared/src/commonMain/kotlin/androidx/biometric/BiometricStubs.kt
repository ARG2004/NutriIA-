package androidx.biometric

import android.content.Context
import androidx.fragment.app.FragmentActivity

object BiometricManager {
    const val BIOMETRIC_SUCCESS = 0
    const val BIOMETRIC_ERROR_NO_HARDWARE = 1
    const val BIOMETRIC_ERROR_HW_UNAVAILABLE = 2
    const val BIOMETRIC_ERROR_NONE_ENROLLED = 11

    object Authenticators {
        const val BIOMETRIC_STRONG = 15
        const val BIOMETRIC_WEAK = 255
        const val DEVICE_CREDENTIAL = 32768
    }

    fun from(context: Context): BiometricManager = this
    fun canAuthenticate(authenticators: Int): Int = BIOMETRIC_SUCCESS
}

class BiometricPrompt(
    activity: FragmentActivity,
    executor: Any?,
    callback: AuthenticationCallback
) {
    abstract class AuthenticationCallback {
        open fun onAuthenticationError(errorCode: Int, errString: CharSequence) {}
        open fun onAuthenticationSucceeded(result: AuthenticationResult) {}
        open fun onAuthenticationFailed() {}
    }

    class AuthenticationResult

    class PromptInfo {
        class Builder {
            fun setTitle(title: CharSequence): Builder = this
            fun setSubtitle(subtitle: CharSequence): Builder = this
            fun setDescription(description: CharSequence): Builder = this
            fun setNegativeButtonText(text: CharSequence): Builder = this
            fun setAllowedAuthenticators(authenticators: Int): Builder = this
            fun build(): PromptInfo = PromptInfo()
        }
    }

    fun authenticate(info: PromptInfo) {}
}
