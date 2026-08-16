package com.example.nutriia.accesibilidad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.auth.RepositorioLogin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccessibilityViewModel : ViewModel() {

    private val repo      = AccessibilityRepository()
    private val loginRepo = RepositorioLogin()

    var ttsManager: NutriTTS? = null
        private set

    private val colaPendiente = mutableListOf<String>()

    // ── Estados observables ───────────────────────────────────────────────────
    val mode = repo.modeFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), AccessibilityMode.NORMAL
    )

    val idioma = repo.langFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), IdiomaVoz.ESPANOL_MX
    )

    val primeraVez = repo.primeraVezFlow.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true
    )

    private val _primeraVezCargada = MutableStateFlow(false)
    val primeraVezCargada = _primeraVezCargada.asStateFlow()

    init {
        viewModelScope.launch {
            repo.primeraVezFlow.first()
            _primeraVezCargada.value = true
        }
        viewModelScope.launch {
            val modoGuardado = repo.modeFlow.first()
            if (modoGuardado == AccessibilityMode.BLIND) {
                iniciarTTS(null)
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

    // ── Cambia idioma ─────────────────────────────────────────────────────────
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
        if (tts == null || !tts.isReady()) { colaPendiente.add(texto); return }
        tts.hablar(texto)
    }

    fun hablarEnCola(texto: String) {
        if (mode.value != AccessibilityMode.BLIND) return
        val tts = ttsManager
        if (tts == null || !tts.isReady()) { colaPendiente.add(texto); return }
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
        ttsManager = NutriTTS(null, idioma.value)
        if (mensajeInicial != null) colaPendiente.add(0, mensajeInicial)

        viewModelScope.launch {
            var intentos = 0
            while (ttsManager?.isReady() == false && intentos < 25) {
                delay(150); intentos++
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

    fun limpiar() {
        liberarTTS()
    }
}