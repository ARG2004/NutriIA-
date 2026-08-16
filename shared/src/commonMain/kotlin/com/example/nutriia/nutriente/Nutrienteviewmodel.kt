package com.example.nutriia.nutriente

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.shared.NutriSharedViewModel
import com.example.nutriia.sueldo.ComidasDiarias
import com.example.nutriia.sueldo.DietaEngine
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.solidos.GuiaEdad
import com.example.nutriia.solidos.guiaParaEdad
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.example.nutriia.utils.FechaUtils

data class DatosInfante(
    val childId: String       = "",
    val meses:   Int          = 0,
    val nivel:   NivelIngreso = NivelIngreso.BASICO,
    val region:  RegionMexico = RegionMexico.CENTRO
)

data class RecomendacionConectada(
    val rangoLabel:  String,
    val caloriasMin: Int,
    val caloriasMax: Int,
    val proteinasG:  Double,
    val grasasPorc:  Double,
    val carbosPorc:  Double,
    val hierroMg:    Double,
    val calcioMg:    Double,
    val vitaminaAug: Int,
    val zincMg:      Double,
    val notas:       List<String>,
    val comidasMin:  Int,
    val comidasMax:  Int,
    val snacksMax:   Int
)

class NutrientesViewModel : ViewModel() {

    // Usa directamente NutrientesRepositorio — el offline lo maneja Firestore
    private val repo = NutrientesRepositorio()

    private var shared: NutriSharedViewModel? = null

    private val _datosInfante = MutableStateFlow(DatosInfante())
    val datosInfante: StateFlow<DatosInfante> = _datosInfante

    private val _fechaSeleccionada = MutableStateFlow(hoy())
    val fechaSeleccionada: StateFlow<String> = _fechaSeleccionada

    private val _registros = MutableStateFlow<List<RegistroNutrientes>>(emptyList())
    val registros: StateFlow<List<RegistroNutrientes>> = _registros

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    val recomendacionActual: StateFlow<RecomendacionConectada> = _datosInfante
        .map { buildRecomendacion(it.meses) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), buildRecomendacion(0))

    val guiaEdadActual: StateFlow<GuiaEdad?> = _datosInfante
        .map { if (it.meses >= 6) guiaParaEdad(it.meses) else null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val totalesDia: StateFlow<Macronutrientes> = _registros.map { lista ->
        lista.fold(Macronutrientes()) { acc, r ->
            acc.copy(
                calorias      = acc.calorias      + r.macros.calorias,
                proteinas     = acc.proteinas     + r.macros.proteinas,
                grasas        = acc.grasas        + r.macros.grasas,
                carbohidratos = acc.carbohidratos + r.macros.carbohidratos
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Macronutrientes())

    val totalesMicrosDia: StateFlow<Micronutrientes> = _registros.map { lista ->
        lista.fold(Micronutrientes()) { acc, r ->
            acc.copy(
                hierro    = acc.hierro    + r.micros.hierro,
                calcio    = acc.calcio    + r.micros.calcio,
                vitaminaA = acc.vitaminaA + r.micros.vitaminaA,
                vitaminaC = acc.vitaminaC + r.micros.vitaminaC,
                zinc      = acc.zinc      + r.micros.zinc
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Micronutrientes())

    // ── Init ──────────────────────────────────────────────────────────────────

    fun init(
        childId:  String,
        meses:    Int,
        nivel:    NivelIngreso          = NivelIngreso.BASICO,
        region:   RegionMexico          = RegionMexico.CENTRO,
        sharedVm: NutriSharedViewModel? = null
    ) {
        shared = sharedVm
        _datosInfante.value = DatosInfante(
            childId = childId,
            meses   = meses,
            nivel   = nivel,
            region  = region
        )

        if (sharedVm != null && sharedVm.planSemanal.value.isEmpty()) {
            sharedVm.generarPlan(meses, nivel, region)
        }

        if (sharedVm != null) {
            viewModelScope.launch {
                sharedVm.planSemanal
                    .filter { it.isNotEmpty() }
                    .collect { /* recomposición automática via StateFlows */ }
            }
        }

        observar(childId)
    }

    // ── Observar Firestore ────────────────────────────────────────────────────
    // FIX: tipo explícito en catch para que el compilador infiera T correctamente

    private fun observar(childId: String) {
        viewModelScope.launch {
            _fechaSeleccionada.collectLatest { fecha ->
                val flow: Flow<List<RegistroNutrientes>> =
                    repo.observarPorHijoYFecha(childId, fecha)
                flow
                    .catch { e -> _error.value = e.message }
                    .collect { lista -> _registros.value = lista }
            }
        }
    }

    fun cambiarFecha(fecha: String) { _fechaSeleccionada.value = fecha }

    // ── CRUD ──────────────────────────────────────────────────────────────────

    fun guardar(registro: RegistroNutrientes) = viewModelScope.launch {
        try { repo.guardar(registro) }
        catch (e: Exception) { _error.value = e.message }
    }

    fun eliminar(registroId: String) = viewModelScope.launch {
        try { repo.eliminar(_datosInfante.value.childId, registroId) }
        catch (e: Exception) { _error.value = e.message }
    }

    fun limpiarError() { _error.value = null }

    // ── Nutrientes del menú semanal ───────────────────────────────────────────

    private fun comidasDelDia(diaSemana: Int): ComidasDiarias? {
        val plan = shared?.planSemanal?.value
        if (!plan.isNullOrEmpty()) {
            return plan[diaSemana.coerceIn(0, plan.lastIndex)].comidas
        }
        val datos = _datosInfante.value
        val planFallback = DietaEngine.generarPlanSemanal(
            meses  = datos.meses,
            nivel  = datos.nivel,
            region = datos.region
        )
        return planFallback.getOrNull(diaSemana.coerceIn(0, planFallback.lastIndex))?.comidas
    }

    fun macrosDelMenuDelDia(diaSemana: Int): Macronutrientes {
        val datos   = _datosInfante.value
        val comidas = comidasDelDia(diaSemana)
        return if (comidas != null && comidas.caloriasEstimadas > 0.0) {
            Macronutrientes(
                calorias      = comidas.caloriasEstimadas,
                proteinas     = comidas.proteinasEstimadas,
                grasas        = comidas.grasasEstimadas,
                carbohidratos = comidas.carbosEstimados
            )
        } else {
            val nutri = NutriEstimadoEngine.estimarDia(datos.meses, datos.nivel, seed = diaSemana)
            Macronutrientes(
                calorias      = nutri.caloriasEstimadas,
                proteinas     = nutri.proteinasEstimadas,
                grasas        = nutri.grasasEstimadas,
                carbohidratos = nutri.carbosEstimados
            )
        }
    }

    fun microsDelMenuDelDia(diaSemana: Int): Micronutrientes {
        val datos   = _datosInfante.value
        val comidas = comidasDelDia(diaSemana)
        return if (comidas != null && comidas.hierroEstimado > 0.0) {
            Micronutrientes(
                hierro    = comidas.hierroEstimado,
                calcio    = comidas.calcioEstimado,
                vitaminaA = comidas.vitaminaAEstimada,
                vitaminaC = comidas.vitaminaCEstimada,
                zinc      = comidas.zincEstimado
            )
        } else {
            val nutri = NutriEstimadoEngine.estimarDia(datos.meses, datos.nivel, seed = diaSemana)
            Micronutrientes(
                hierro    = nutri.hierroEstimado,
                calcio    = nutri.calcioEstimado,
                vitaminaA = nutri.vitaminaAEstimada,
                vitaminaC = nutri.vitaminaCEstimada,
                zinc      = nutri.zincEstimado
            )
        }
    }

    // ── Helpers privados ──────────────────────────────────────────────────────

    private fun buildRecomendacion(mesesEdad: Int): RecomendacionConectada {
        val macro = DietaEngine.macrosPorEdad(mesesEdad)
        val rango = DietaEngine.etapaLabel(mesesEdad)
        val oms   = recomendacionesOMS[rangoEdadDesde(mesesEdad)]
        return RecomendacionConectada(
            rangoLabel  = rango,
            caloriasMin = macro.caloriasMin,
            caloriasMax = macro.caloriasMax,
            proteinasG  = macro.proteinasG,
            grasasPorc  = macro.grasasPorc,
            carbosPorc  = macro.carbosPorc,
            hierroMg    = macro.hierroMg,
            calcioMg    = macro.calcioMg,
            vitaminaAug = macro.vitaminaAug,
            zincMg      = macro.zincMg,
            notas       = oms?.notas ?: emptyList(),
            comidasMin  = oms?.comidasPorDia?.first ?: 2,
            comidasMax  = oms?.comidasPorDia?.last  ?: 3,
            snacksMax   = oms?.snacksPorDia?.last   ?: 0
        )
    }

    private fun hoy(): String = FechaUtils.fechaActual()
}