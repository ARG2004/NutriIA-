package com.example.nutriia.embarazo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.ginecologo.GinecologoRepository
import com.example.nutriia.ginecologo.VinculacionEmbarazo
import com.example.nutriia.ginecologo.EstadoVinculacionEmbarazo
import com.example.nutriia.nutriente.RegistroNutrientes
import com.example.nutriia.alerta.Alerta
import com.example.nutriia.alerta.TipoAlerta
import com.example.nutriia.firebase.auth.FirebaseAuth
import com.example.nutriia.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import com.example.nutriia.utils.FechaUtils

data class AccionPendiente(
    val id: String,
    val tituloEs: String,
    val tituloEn: String,
    val subtituloEs: String,
    val subtituloEn: String,
    val modulo: String // "NUTRICION", "PESO", "SINTOMAS", "CITAS", "GINECOLOGO", "CHAT"
)

data class EstadoModulo(
    val subtituloEs: String,
    val subtituloEn: String,
    val badge: String? = null
)

class EmbarazoDashboardViewModel : ViewModel() {

    // 1. Tabla de comparación de tamaños del bebé (Semanas 1-40)
    private val tamanosEs = mapOf(
        1 to "una semilla de amapola", 2 to "una semilla de amapola", 3 to "una semilla de amapola",
        4 to "una semilla de sésamo", 5 to "una semilla de manzana", 6 to "un guisante",
        7 to "un arándano", 8 to "una frambuesa", 9 to "una uva",
        10 to "un kumquat", 11 to "un higo", 12 to "una lima",
        13 to "una vaina de guisante", 14 to "un limón", 15 to "una manzana",
        16 to "un aguacate", 17 to "una granada", 18 to "un camote",
        19 to "un mango", 20 to "un plátano", 21 to "una zanahoria",
        22 to "una papaya", 23 to "una toronja", 24 to "un elote",
        25 to "una coliflor", 26 to "una lechuga", 27 to "una col",
        28 to "una berenjena", 29 to "una calabaza butternut", 30 to "un pepino",
        31 to "una piña", 32 to "un melón cantalupo", 33 to "un apio",
        34 to "un melón", 35 to "un coco", 36 to "una lechuga romana",
        37 to "una acelga", 38 to "un puerro", 39 to "una sandía",
        40 to "una calabaza"
    )

    private val tamanosEn = mapOf(
        1 to "a poppy seed", 2 to "a poppy seed", 3 to "a poppy seed",
        4 to "a sesame seed", 5 to "an apple seed", 6 to "a sweet pea",
        7 to "a blueberry", 8 to "a raspberry", 9 to "a grape",
        10 to "a kumquat", 11 to "a fig", 12 to "a lime",
        13 to "a pea pod", 14 to "a lemon", 15 to "an apple",
        16 to "an avocado", 17 to "a pomegranate", 18 to "a sweet potato",
        19 to "a mango", 20 to "a banana", 21 to "a carrot",
        22 to "a papaya", 23 to "a grapefruit", 24 to "an ear of corn",
        25 to "a cauliflower", 26 to "a head of lettuce", 27 to "a cabbage",
        28 to "an eggplant", 29 to "a butternut squash", 30 to "a cucumber",
        31 to "a pineapple", 32 to "a cantaloupe", 33 to "a stalk of celery",
        34 to "a honeydew melon", 35 to "a coconut", 36 to "a romaine lettuce",
        37 to "a swiss chard", 38 to "a leek", 39 to "a watermelon",
        40 to "a pumpkin"
    )

    fun obtenerTamanoBebe(semana: Int, esIngles: Boolean): String {
        val mapa = if (esIngles) tamanosEn else tamanosEs
        return mapa[semana] ?: (if (esIngles) "a poppy seed" else "una semilla de amapola")
    }

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gineRepo = GinecologoRepository()

    private val _semanaActual = MutableStateFlow(1)
    val semanaActual: StateFlow<Int> = _semanaActual.asStateFlow()

    private val _perfilEmbarazo = callbackFlow<PerfilEmbarazo?> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("usuarios")
            .document(uid)
            .collection("perfilEmbarazo")
            .document("unico")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                val p = snap?.let {
                    it.dataAs<PerfilEmbarazo>() ?: PerfilEmbarazo(
                        semanas = it.getLong("semanas")?.toInt() ?: 1,
                        condiciones = it.getAs<List<String>>("condiciones") ?: emptyList(),
                        preferencias = it.getAs<List<String>>("preferencias") ?: emptyList(),
                        fechaUltimaMenstruacion = it.getString("fechaUltimaMenstruacion") ?: "",
                        nivelIngreso = com.example.nutriia.sueldo.NivelIngreso.fromIndex(it.getLong("nivelIngreso")?.toInt() ?: 0),
                        region = it.getString("region")?.let { r -> com.example.nutriia.sueldo.RegionMexico.entries.firstOrNull { reg -> reg.name == r } } ?: com.example.nutriia.sueldo.RegionMexico.CENTRO,
                        allergiesDetail = it.getString("allergiesDetail") ?: "",
                        edad = it.getLong("edad")?.toInt() ?: 0,
                        tallaM = it.getDouble("tallaM") ?: 0.0,
                        pesoPregestacionalKg = it.getDouble("pesoPregestacionalKg") ?: 0.0,
                        esGemelar = it.getBoolean("esGemelar") ?: false,
                        otrasCondicionesTexto = it.getString("otrasCondicionesTexto") ?: ""
                    )
                }
                trySend(p)
            }
        awaitClose { listener.remove() }
    }.catch { emit(null) }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val perfilEmbarazo: StateFlow<PerfilEmbarazo?> = _perfilEmbarazo

    private val _registrosPeso = callbackFlow<List<RegistroPesoEmbarazo>> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("usuarios")
            .document(uid)
            .collection("perfilEmbarazo")
            .document("unico")
            .collection("registrosPeso")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.data?.let { RegistroPesoEmbarazo.fromMap(doc.id, it) }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { emit(emptyList()) }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registrosPeso: StateFlow<List<RegistroPesoEmbarazo>> = _registrosPeso

    private val _registrosSintomas = callbackFlow<List<RegistroSintomasEmbarazo>> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("usuarios")
            .document(uid)
            .collection("perfilEmbarazo")
            .document("unico")
            .collection("registrosSintomas")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.data?.let { RegistroSintomasEmbarazo.fromMap(doc.id, it) }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { emit(emptyList()) }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val registrosSintomas: StateFlow<List<RegistroSintomasEmbarazo>> = _registrosSintomas

    fun guardarRegistroPeso(registro: RegistroPesoEmbarazo) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val ref = db.collection("usuarios")
                .document(uid)
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("registrosPeso")
            val id = if (registro.id.isBlank()) ref.document().id else registro.id
            ref.document(id).set(registro.copy(id = id).toMap())
        }
    }

    fun eliminarRegistroPeso(id: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            db.collection("usuarios")
                .document(uid)
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("registrosPeso")
                .document(id)
                .delete()
        }
    }

    fun guardarRegistroSintomas(registro: RegistroSintomasEmbarazo) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val ref = db.collection("usuarios")
                .document(uid)
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("registrosSintomas")
            val id = if (registro.id.isBlank()) ref.document().id else registro.id
            ref.document(id).set(registro.copy(id = id).toMap())
        }
    }

    fun eliminarRegistroSintomas(id: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            db.collection("usuarios")
                .document(uid)
                .collection("perfilEmbarazo")
                .document("unico")
                .collection("registrosSintomas")
                .document(id)
                .delete()
        }
    }

    fun actualizarSemana(semana: Int) {
        _semanaActual.value = semana
    }

    private fun hoy(): String = FechaUtils.fechaActual()

    // 2. Nueva sección "Hoy para ti": Acciones pendientes basadas en datos de hoy de Firestore
    private val _nutrientesHoy = callbackFlow<List<RegistroNutrientes>> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("usuarios")
            .document(uid)
            .collection("perfilEmbarazo")
            .document("unico")
            .collection("embarazoNutrientes")
            .whereEqualTo("fecha", hoy())
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.data?.let { RegistroNutrientes.fromMap(it) }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { emit(emptyList()) }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accionesHoy: StateFlow<List<AccionPendiente>> = _nutrientesHoy.map { nutriList ->
        val numComidas = nutriList.size
        val comidaSubEs = when {
            numComidas == 0 -> "0 de 4 comidas registradas"
            numComidas >= 4 -> "¡Comidas del día completadas!"
            else -> "$numComidas de 4 comidas registradas"
        }
        val comidaSubEn = when {
            numComidas == 0 -> "0 of 4 meals logged"
            numComidas >= 4 -> "All meals logged!"
            else -> "$numComidas of 4 meals logged"
        }

        val tieneSuplemento = nutriList.any { registro ->
            val desc = registro.alimento.lowercase()
            desc.contains("ácido fólico") || desc.contains("acido folico") ||
                    desc.contains("suplemento") || desc.contains("vitamina") ||
                    desc.contains("hierro")
        }
        val supSubEs = if (tieneSuplemento) "Completado hoy" else "Pendiente hoy (ácido fólico)"
        val supSubEn = if (tieneSuplemento) "Completed today" else "Pending today (folic acid)"

        listOf(
            AccionPendiente(
                id = "1",
                tituloEs = "Registra tu comida de hoy",
                tituloEn = "Log your meals today",
                subtituloEs = comidaSubEs,
                subtituloEn = comidaSubEn,
                modulo = "NUTRICION"
            ),
            AccionPendiente(
                id = "2",
                tituloEs = "Toma tus suplementos",
                tituloEn = "Take your supplements",
                subtituloEs = supSubEs,
                subtituloEn = supSubEn,
                modulo = "NUTRICION"
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 3. Grid de módulos con badges y subtítulos en tiempo real
    private val _vinculacionGinecologo = gineRepo.observarVinculacionDeLaMama()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            _vinculacionGinecologo.collect { vinculacion ->
                val uid = auth.currentUser?.uid ?: return@collect
                if (vinculacion != null && vinculacion.estado == EstadoVinculacionEmbarazo.ACTIVO && vinculacion.proximaCitaFecha.isNotBlank()) {
                    // Convert date YYYY-MM-DD to DD/MM/YYYY
                    val rawDate = vinculacion.proximaCitaFecha
                    val parts = rawDate.split("-")
                    val alertDate = if (parts.size == 3) "${parts[2]}/${parts[1]}/${parts[0]}" else rawDate
                    
                    val alertaId = "cita_${vinculacion.id}"
                    val alertaMap = mapOf(
                        "id" to alertaId,
                        "childId" to "",
                        "childName" to "Mi Embarazo",
                        "tipo" to "CITA_MEDICA",
                        "titulo" to "Cita con Ginecólogo/a",
                        "descripcion" to "Consulta: ${vinculacion.proximaCitaMotivo}",
                        "hora" to vinculacion.proximaCitaHora,
                        "fechaUnica" to alertDate,
                        "activa" to true
                    )
                    
                    db.collection("usuarios")
                        .document(uid)
                        .collection("perfilEmbarazo")
                        .document("unico")
                        .collection("alertas")
                        .document(alertaId)
                        .set(alertaMap)
                } else if (vinculacion != null) {
                    val alertaId = "cita_${vinculacion.id}"
                    db.collection("usuarios")
                        .document(uid)
                        .collection("perfilEmbarazo")
                        .document("unico")
                        .collection("alertas")
                        .document(alertaId)
                        .delete()
                }
            }
        }
    }

    private val _alertasActivas = callbackFlow<List<Alerta>> {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val listener = db.collection("usuarios")
            .document(uid)
            .collection("perfilEmbarazo")
            .document("unico")
            .collection("alertas")
            .whereEqualTo("activa", true)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents?.mapNotNull { doc ->
                    doc.data?.let { Alerta.fromMap(it) }
                } ?: emptyList()
                trySend(list)
            }
        awaitClose { listener.remove() }
    }.catch { emit(emptyList()) }
     .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val estadoModulos: StateFlow<Map<String, EstadoModulo>> = combine(
        combine(_semanaActual, _vinculacionGinecologo, _nutrientesHoy, _alertasActivas) { a, b, c, d ->
            listOf(a, b, c, d)
        },
        _perfilEmbarazo,
        _registrosPeso
    ) { list4, perfil, registros ->
        val semana = list4[0] as Int
        val gine = list4[1] as VinculacionEmbarazo?
        val nutriList = list4[2] as List<RegistroNutrientes>
        val alertList = list4[3] as List<Alerta>

        // 1. Ginecologo
        val gineEstado = if (gine == null) {
            EstadoModulo("Sin ginecólogo vinculado", "No gynecologist linked", null)
        } else {
            when (gine.estado) {
                EstadoVinculacionEmbarazo.ACTIVO -> EstadoModulo(
                    "${gine.ginecologoNombre} · Activo",
                    "${gine.ginecologoNombre} · Active",
                    null
                )
                EstadoVinculacionEmbarazo.PENDIENTE -> EstadoModulo(
                    "${gine.ginecologoNombre} · Pendiente",
                    "${gine.ginecologoNombre} · Pending",
                    "1"
                )
                else -> EstadoModulo("Sin ginecólogo vinculado", "No gynecologist linked", null)
            }
        }

        // 2. Nutricion
        val totalKcal = nutriList.sumOf { it.macros.calorias }.toInt()
        val numComidas = nutriList.size
        val nutriEstado = EstadoModulo(
            subtituloEs = "$totalKcal kcal registradas",
            subtituloEn = "$totalKcal kcal logged",
            badge = if (numComidas > 0) "$numComidas" else null
        )

        // 3. Peso
        val ultimoRegistro = registros.maxByOrNull { it.semanaGestacion }
        val pesoEstado = if (ultimoRegistro != null && perfil != null) {
            val imc = perfil.imcPregestacional
            val rango = GananciaPesoCalculator.rangoAjustado(imc, perfil.edad, perfil.tallaM)
            val gananciaEsperada = GananciaPesoCalculator.gananciaEsperadaAcumulada(ultimoRegistro.semanaGestacion, rango)
            val gananciaActual = ultimoRegistro.pesoActualKg - perfil.pesoPregestacionalKg
            val estado = if (perfil.esGemelar) {
                GananciaPesoCalculator.EstadoGanancia.EN_RANGO
            } else {
                GananciaPesoCalculator.evaluarEstado(gananciaActual, gananciaEsperada)
            }
            val hasBadge = estado != GananciaPesoCalculator.EstadoGanancia.EN_RANGO && !perfil.esGemelar
            
            EstadoModulo(
                subtituloEs = "${ultimoRegistro.pesoActualKg} kg · Sem. ${ultimoRegistro.semanaGestacion}",
                subtituloEn = "${ultimoRegistro.pesoActualKg} kg · Wk. ${ultimoRegistro.semanaGestacion}",
                badge = if (hasBadge) "!" else null
            )
        } else {
            EstadoModulo("Registra tu peso", "Log your weight", "!")
        }

        // 4. Sintomas (Sintoma principal y badge con número total de síntomas para la semana)
        val infoSintoma = obtenerInfoSintomas(semana)
        val principalEs = infoSintoma.sintomasEs.firstOrNull() ?: "Sin síntomas comunes"
        val principalEn = infoSintoma.sintomasEn.firstOrNull() ?: "No common symptoms"
        val sintEstado = EstadoModulo(
            subtituloEs = principalEs,
            subtituloEn = principalEn,
            badge = if (infoSintoma.sintomasEs.isNotEmpty()) "${infoSintoma.sintomasEs.size}" else null
        )

        // 5. Citas (Siguiente cita médica programada activa)
        val appointments = alertList.filter { it.tipo == TipoAlerta.CITA_MEDICA }
        val citasEstado = if (appointments.isEmpty()) {
            EstadoModulo("Sin citas programadas", "No scheduled appts", null)
        } else {
            val nearest = appointments.sortedWith(compareBy<Alerta> { it.fechaUnica ?: "31/12/9999" }.thenBy { it.hora }).first()
            val dateStr = nearest.fechaUnica ?: "Semanal"
            val textEs = "Próxima: $dateStr a las ${nearest.hora}"
            val textEn = "Next: $dateStr at ${nearest.hora}"
            EstadoModulo(textEs, textEn, "${appointments.size}")
        }

        // 6. Preguntame
        val pregEstado = EstadoModulo(
            subtituloEs = "NutriBot activo",
            subtituloEn = "NutriBot online",
            badge = null
        )

        mapOf(
            "ginecologo" to gineEstado,
            "nutricion" to nutriEstado,
            "peso" to pesoEstado,
            "sintomas" to sintEstado,
            "citas" to citasEstado,
            "preguntame" to pregEstado
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun guardarPerfilEmbarazo(perfil: PerfilEmbarazo) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            db.collection("usuarios")
                .document(uid)
                .collection("perfilEmbarazo")
                .document("unico")
                .set(perfil.toMap())
        }
    }
}

