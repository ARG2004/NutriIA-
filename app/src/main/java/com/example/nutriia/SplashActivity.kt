package com.example.nutriia

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Inicializa el splash nativo para poder ocultar el icono circular del sistema
        installSplashScreen()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val logo = findViewById<ImageView>(R.id.splash_logo)
        val loadingContainer = findViewById<View>(R.id.splash_loading_container)

        // Preparar visibilidad y posiciones iniciales para la entrada suave
        logo?.alpha = 0f
        logo?.scaleX = 0.88f
        logo?.scaleY = 0.88f

        loadingContainer?.alpha = 0f
        loadingContainer?.translationY = 30f

        // Animación de entrada elegante para el logo
        logo?.animate()
            ?.alpha(1f)
            ?.scaleX(1f)
            ?.scaleY(1f)
            ?.setDuration(650)
            ?.setInterpolator(DecelerateInterpolator(1.5f))
            ?.withEndAction {
                iniciarRespiracionLogo(logo)
            }
            ?.start()

        // Animación de entrada suave para la barra de progreso y texto
        loadingContainer?.animate()
            ?.alpha(1f)
            ?.translationY(0f)
            ?.setStartDelay(150)
            ?.setDuration(500)
            ?.setInterpolator(DecelerateInterpolator())
            ?.start()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(
                    OVERRIDE_TRANSITION_OPEN,
                    R.anim.splash_fade_in,
                    R.anim.splash_fade_out
                )
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(R.anim.splash_fade_in, R.anim.splash_fade_out)
            }

            finish()
        }, 1600)
    }

    private fun iniciarRespiracionLogo(logo: View) {
        logo.animate()
            .scaleX(1.035f)
            .scaleY(1.035f)
            .setDuration(900)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                logo.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(900)
                    .setInterpolator(AccelerateDecelerateInterpolator())
                    .start()
            }
            .start()
    }
}