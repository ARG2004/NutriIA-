package com.example.nutriia.accesibilidad

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.auth.RepositorioLogin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccessibilityViewModel(app: Application) : AndroidViewModel(app) {

    private val repo      = AccessibilityRepository(app)
    private val loginRepo = RepositorioLogin(app)   // ← context agregado

    var ttsManager: NutriTTS? = null
        private set

    private val colaPendiente = mutableListOf<String>()

    // ── Estados observables ───────────────────────────────────────────────────
    val mode = repo.modeFlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, AccessibilityMode.NORMAL
    )

    val idioma = repo.langFlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, IdiomaVoz.ESPANOL_MX
    )

    val primeraVez = repo.primeraVezFlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, true
    )

    private val _primeraVezCargada = MutableStateFlow(false)
    val primeraVezCargada = _primeraVezCargada.asStateFlow()

    init {
        viewModelScope.launch {
            repo.primeraVezFlow.first()
            _primeraVezCargada.value = true
        }
        viewModelScope.launch {
            repo.modeFlow.collect { modoGuardado ->
                if (modoGuardado == AccessibilityMode.BLIND) {
                    if (ttsManager == null) iniciarTTS(null)
                }
            }
        }
    }

    fun marcarPrimeraVezCompletada() {
        viewModelScope.launch { repo.marcarPrimeraVezCompletada() }
    }

    // ── Cambia modo ───────────────────────────────────────────────────────────
    fun setMode(modo: AccessibilityMode) {
        viewModelScope.launch {
            repo.saveMode(modo)
            val uid = loginRepo.obtenerUsuarioActual()?.uid
            if (uid != null) loginRepo.guardarModoAccesibilidad(uid, modo)
        }
        when (modo) {
            AccessibilityMode.BLIND -> iniciarTTS(Voz.MODO_CIEGO)
            else -> liberarTTS()
        }
    }

    // ── Cambia idioma (sin INDIGENA) ──────────────────────────────────────────
    fun setIdioma(nuevoIdioma: IdiomaVoz) {
        viewModelScope.launch { repo.saveLang(nuevoIdioma) }
        ttsManager?.cambiarIdioma(nuevoIdioma)
        val texto = when (nuevoIdioma) {
            IdiomaVoz.ESPANOL_MX -> "Idioma cambiado a Español Latinoamérica."
            IdiomaVoz.ESPANOL_US -> "Idioma cambiado a Español Estados Unidos."
            IdiomaVoz.INGLES     -> "Language changed to English."
        }
        hablar(texto)
    }

    // ── Habla con cola si TTS no listo ────────────────────────────────────────
    fun hablar(texto: String) {
        if (mode.value != AccessibilityMode.BLIND) return
        val tts = ttsManager
        if (tts == null) {
            iniciarTTS(texto)
            return
        }
        if (!tts.isReady()) {
            colaPendiente.add(texto)
            return
        }
        tts.hablar(texto)
    }

    fun hablarEnCola(texto: String) {
        if (mode.value != AccessibilityMode.BLIND) return
        val tts = ttsManager
        if (tts == null) {
            iniciarTTS(texto)
            return
        }
        if (!tts.isReady()) {
            colaPendiente.add(texto)
            return
        }
        tts.hablarEnCola(texto)
    }

    fun silenciar() = ttsManager?.silenciar()

    // ── Sincroniza Firebase → DataStore ──────────────────────────────────────
    fun sincronizarDesdeFirebase() {
        viewModelScope.launch {
            val uid = loginRepo.obtenerUsuarioActual()?.uid ?: return@launch
            val modoRemoto = loginRepo.cargarModoAccesibilidad(uid)
            repo.saveMode(modoRemoto)
            if (modoRemoto == AccessibilityMode.BLIND) iniciarTTS(null)
        }
    }

    // ── Inicializa TTS y drena cola ───────────────────────────────────────────
    fun iniciarTTS(mensajeInicial: String?) {
        if (ttsManager != null) {
            mensajeInicial?.let { hablar(it) }
            return
        }
        ttsManager = NutriTTS(getApplication(), idioma.value)
        if (mensajeInicial != null) colaPendiente.add(mensajeInicial)

        viewModelScope.launch {
            var intentos = 0
            while (ttsManager?.isReady() == false && intentos < 30) {
                delay(100); intentos++
            }
            val cola = colaPendiente.toList()
            colaPendiente.clear()
            cola.forEach { ttsManager?.hablar(it) }
        }
    }

    private fun liberarTTS() {
        colaPendiente.clear()
        ttsManager?.liberar()
        ttsManager = null
    }

    override fun onCleared() { super.onCleared(); liberarTTS() }
}