package com.example.nutriia.expediente

import com.example.nutriia.platform.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.shared.NutriSharedViewModel
import com.example.nutriia.solidos.ReaccionAlimento
import com.example.nutriia.sueldo.DietaEngine
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.sueldo.TipoComida
import com.example.nutriia.crecimiento.MedicionCrecimiento
import com.example.nutriia.crecimiento.Sexo
import com.example.nutriia.crecimiento.interpretarIMC
import com.example.nutriia.crecimiento.InterpretacionIMC
import com.example.nutriia.offline.OfflineManager
import com.example.nutriia.firebase.auth.FirebaseAuth
import com.example.nutriia.firebase.firestore.FirebaseFirestore
import com.example.nutriia.firebase.firestore.ListenerRegistration
import com.example.nutriia.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.example.nutriia.firebase.firestore.await
import com.example.nutriia.utils.FechaUtils

// ═══════════════════════════════════════════════════════════════════════════
// MODELOS DE ESTADO PARA EL EXPEDIENTE
// ═══════════════════════════════════════════════════════════════════════════

data class NotaConsulta(
    val id:          String = "",
    val texto:       String = "",
    val autorNombre: String = "Nutriólogo",
    val fechaMs:     Long   = 0L
) {
    val fechaStr: String get() {
        if (fechaMs == 0L) return "Reciente"
        return FechaUtils.formatearFecha(fechaMs)
    }
}

data class AlimentoIntroducido(
    val nombre:  String = "",
    val estado:  String = "", // "Aceptado", "Rechazado", "En prueba"
    val fechaMs: Long   = 0L
)

// ── entrada guardada en 'consultas' desde el tab Alimentación ─────────────
data class EntradaAlimentacion(
    val id:         String = "",
    val tipo:       String = "", // "receta_nutriologo" | "observacion_alimentacion"
    val titulo:     String = "",
    val contenido:  String = "",
    val autorNombre:String = "Nutriólogo",
    val fechaMs:    Long   = 0L
) {
    val fechaStr: String get() {
        if (fechaMs == 0L) return "Reciente"
        return FechaUtils.formatearFecha(fechaMs)
    }
    val esReceta: Boolean get() = tipo == "receta_nutriologo"
}

data class ExpedienteUiState(
    val cargando:              Boolean                   = true,
    val error:                 String?                   = null,
    val childNombre:           String                    = "",
    val padreNombre:           String                    = "",
    val birthDate:             String                    = "",
    val weightKg:              Double                    = 0.0,
    val heightCm:              Double                    = 0.0,
    val hasAllergies:          Boolean                   = false,
    val notasConsulta:         List<NotaConsulta>        = emptyList(),
    val alimentosIntrod:       List<AlimentoIntroducido> = emptyList(),
    val mostrarFormaNota:      Boolean                   = false,
    val guardandoNota:         Boolean                   = false,
    val exito:                 String?                   = null,
    val edadMeses:             Int                       = 0,
    val ultimaMedicionCrec:    String                    = "",

    // ── NUEVO: clasificación socioeconómica y región ───────────────────────
    val nivelIngreso:          NivelIngreso              = NivelIngreso.BASICO,
    val region:                RegionMexico              = RegionMexico.PUEBLA,

    // ── Tab Alimentación ──────────────────────────────────────────────────
    val tabSeleccionado:       Int                       = 0,
    val planSemanal:           List<PlanDiaResumen>      = emptyList(),
    val recetasSugeridas:      List<com.example.nutriia.sueldo.RecetaMexicana> = emptyList(),
    val entradasAlimentacion:  List<EntradaAlimentacion> = emptyList(),
    val mostrarFormaAlim:      Boolean                   = false,
    val tipoEntradaAlim:       String                    = "receta_nutriologo",
    val guardandoEntradaAlim:  Boolean                   = false,
    val historialCrecimiento:  List<MedicionCrecimiento> = emptyList(),
)

// Modelo ligero para mostrar el plan en el expediente
data class PlanDiaResumen(
    val diaSemana:  String,
    val desayuno:   String,
    val almuerzo:   String,
    val colacion1:  String,
    val colacion2:  String,
    val cena:       String
)

// ═══════════════════════════════════════════════════════════════════════════
// PacienteExpedienteViewModel
// ═══════════════════════════════════════════════════════════════════════════

class PacienteExpedienteViewModel : ViewModel() {

    private val db   = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _ui = MutableStateFlow(ExpedienteUiState())
    val ui = _ui.asStateFlow()

    private var _ownerUid = ""
    private var _childId  = ""

    // ── Listeners en tiempo real (cleanup en onCleared) ──────────────────
    private var notasListener:       ListenerRegistration? = null
    private var alimentosListener:   ListenerRegistration? = null
    private var entradasListener:    ListenerRegistration? = null
    private var crecimientoListener: ListenerRegistration? = null

    override fun onCleared() {
        super.onCleared()
        notasListener?.remove()
        alimentosListener?.remove()
        entradasListener?.remove()
        crecimientoListener?.remove()
    }

    fun cargar(
        ownerUid: String,
        childId: String,
        childNombre: String,
        padreNombre: String,
        sharedViewModel: NutriSharedViewModel? = null
    ) {
        _ownerUid = ownerUid
        _childId  = childId

        viewModelScope.launch {
            _ui.value = _ui.value.copy(cargando = true, error = null)

            var nacimiento = ""
            var nivelIngreso = NivelIngreso.BASICO
            var region = RegionMexico.PUEBLA
            var basePeso = 0.0
            var baseTalla = 0.0
            var hasAllergiesVal = false

            try {
                val hijoDoc = db.collection("usuarios")
                    .document(ownerUid)
                    .collection("hijos")
                    .document(childId)
                    .get().await()

                val doc = hijoDoc as? com.example.nutriia.firebase.firestore.DocumentSnapshot
                if (doc != null && doc.exists) {
                    nacimiento = doc.getString("birthDate") ?: ""
                    nivelIngreso = doc.getString("nivelIngreso")
                        ?.let { runCatching { NivelIngreso.valueOf(it) }.getOrDefault(NivelIngreso.BASICO) }
                        ?: NivelIngreso.BASICO

                    region = doc.getString("region")
                        ?.let { runCatching { RegionMexico.valueOf(it) }.getOrDefault(RegionMexico.PUEBLA) }
                        ?: RegionMexico.PUEBLA

                    basePeso = doc.data["weightKg"]?.toString()?.toDoubleOrNull() ?: 0.0
                    baseTalla = doc.data["heightCm"]?.toString()?.toDoubleOrNull() ?: 0.0
                    hasAllergiesVal = doc.getBoolean("hasAllergies") ?: false
                }
            } catch (e: Exception) {
                com.example.nutriia.platform.Log.w("Expediente", "Fallo al obtener perfil base del hijo: ${e.message}")
            }

            val meses = calcularEdadMeses(nacimiento)

            // ── 3. Sólidos → LISTENER EN TIEMPO REAL ─────────────────
            iniciarListenerAlimentos(ownerUid, childId, sharedViewModel)

            // ── 4. Notas clínicas → LISTENER EN TIEMPO REAL ──────────
            iniciarListenerNotas(ownerUid, childId)

            // ── 5. Entradas alimentación → LISTENER EN TIEMPO REAL ───
            iniciarListenerEntradas(ownerUid, childId)

            // ── 6. Historial de crecimiento → LISTENER EN TIEMPO REAL ────────
            iniciarListenerCrecimiento(ownerUid, childId, basePeso, baseTalla)

            // ── 7. Plan semanal + recetas sugeridas (one-shot) ────────
            val nombresTolerados = _ui.value.alimentosIntrod
                .filter { it.estado == "Aceptado" || it.estado == "En prueba" }
                .map { it.nombre }

            val edadParaMotor = meses.coerceAtLeast(6)

            val planDieta = try {
                DietaEngine.generarPlanSemanal(
                    meses                = edadParaMotor,
                    nivel                = nivelIngreso,
                    region               = region,
                    alergenosNiño        = emptyList(),
                    alimentosRegistrados = nombresTolerados
                )
            } catch (e: Exception) { emptyList() }

            val planResumen = planDieta.map { d ->
                PlanDiaResumen(
                    diaSemana = d.diaSemana,
                    desayuno  = d.comidas.desayuno,
                    almuerzo  = d.comidas.almuerzo,
                    colacion1 = d.comidas.colacion1,
                    colacion2 = d.comidas.colacion2,
                    cena      = d.comidas.cena
                )
            }

            val recetasSug = try {
                TipoComida.entries.flatMap { tipo ->
                    DietaEngine.recetasPorPerfil(
                        meses                = edadParaMotor,
                        nivel                = nivelIngreso,
                        tipo                 = tipo,
                        region               = region,
                        alergenosNiño        = emptyList(),
                        alimentosRegistrados = nombresTolerados
                    )
                }.distinctBy { it.nombre }.take(12)
            } catch (e: Exception) { emptyList() }

            // ── 8. Consolidar estado (se actualizan por listeners) ──
            _ui.value = _ui.value.copy(
                cargando             = false,
                childNombre          = childNombre,
                padreNombre          = padreNombre,
                birthDate            = nacimiento,
                weightKg             = basePeso,
                heightCm             = baseTalla,
                hasAllergies         = hasAllergiesVal,
                edadMeses            = meses,
                nivelIngreso         = nivelIngreso,
                region               = region,
                planSemanal          = planResumen,
                recetasSugeridas     = recetasSug
            )
        }
    }

    // ── Listeners en tiempo real ──────────────────────────────────────────

    private fun iniciarListenerAlimentos(ownerUid: String, childId: String, sharedViewModel: NutriSharedViewModel?) {
        alimentosListener?.remove()
        alimentosListener = db.collection("usuarios")
            .document(ownerUid)
            .collection("hijos")
            .document(childId)
            .collection("solidos")
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("Expediente", "Error listener alimentos", err); return@addSnapshotListener }
                val solidosMapeados = snap?.documents?.mapNotNull { doc ->
                    runCatching {
                        val reaccionStr = doc.getString("reaccion") ?: ReaccionAlimento.NINGUNA.name
                        val reaccion = runCatching { ReaccionAlimento.valueOf(reaccionStr) }
                            .getOrDefault(ReaccionAlimento.NINGUNA)
                        AlimentoIntroducido(
                            nombre  = doc.getString("nombre") ?: return@runCatching null,
                            estado  = when (reaccion) {
                                ReaccionAlimento.ACEPTADO -> "Aceptado"
                                ReaccionAlimento.NINGUNA,
                                ReaccionAlimento.LEVE     -> "En prueba"
                                ReaccionAlimento.ALERGIA,
                                ReaccionAlimento.RECHAZO  -> "Rechazado"
                            },
                            fechaMs = doc.getTimestamp("creadoEn")?.time ?: 0L
                        )
                    }.getOrNull()
                }?.sortedByDescending { it.fechaMs } ?: emptyList()

                _ui.value = _ui.value.copy(alimentosIntrod = solidosMapeados)

                sharedViewModel?.actualizarDesdeExpediente(
                    solidosMapeados.filter { it.estado == "Aceptado" }.map { it.nombre }
                )
                sharedViewModel?.cargarPerfil(ownerUid, childId)
            }
    }

    private fun iniciarListenerNotas(ownerUid: String, childId: String) {
        notasListener?.remove()
        notasListener = db.collection("usuarios")
            .document(ownerUid)
            .collection("hijos")
            .document(childId)
            .collection("consultas")
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("Expediente", "Error listener notas", err); return@addSnapshotListener }
                val notas = snap?.documents?.mapNotNull { d ->
                    val tipo = d.getString("tipo") ?: ""
                    if (tipo != "nota_nutriologo") return@mapNotNull null
                    NotaConsulta(
                        id          = d.id,
                        texto       = d.getString("texto") ?: d.getString("contenido") ?: "",
                        autorNombre = d.getString("autorNombre") ?: "Nutriólogo",
                        fechaMs     = d.getTimestamp("creadoEn")?.time ?: 0L
                    )
                }?.sortedByDescending { it.fechaMs } ?: emptyList()
                _ui.value = _ui.value.copy(notasConsulta = notas)
            }
    }

    private fun iniciarListenerEntradas(ownerUid: String, childId: String) {
        entradasListener?.remove()
        entradasListener = db.collection("usuarios")
            .document(ownerUid)
            .collection("hijos")
            .document(childId)
            .collection("consultas")
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("Expediente", "Error listener entradas", err); return@addSnapshotListener }
                val entradas = snap?.documents?.mapNotNull { d ->
                    val tipo = d.getString("tipo") ?: ""
                    if (tipo != "receta_nutriologo" && tipo != "observacion_alimentacion") return@mapNotNull null
                    EntradaAlimentacion(
                        id          = d.id,
                        tipo        = tipo,
                        titulo      = d.getString("titulo") ?: "",
                        contenido   = d.getString("contenido") ?: d.getString("texto") ?: "",
                        autorNombre = d.getString("autorNombre") ?: "Nutriólogo",
                        fechaMs     = d.getTimestamp("creadoEn")?.time ?: 0L
                    )
                }?.sortedByDescending { it.fechaMs } ?: emptyList()
                _ui.value = _ui.value.copy(entradasAlimentacion = entradas)
            }
    }

    // ── Notas clínicas ────────────────────────────────────────────────────

    fun guardarNota(ownerUid: String, childId: String, texto: String) {
        val currentUser = auth.currentUser ?: return
        if (texto.isBlank()) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(guardandoNota = true)
            try {
                val docId = db.collection("usuarios").document(ownerUid)
                    .collection("hijos").document(childId)
                    .collection("consultas").document().id

                val data = mapOf(
                    "id"          to docId,
                    "texto"       to texto,
                    "contenido"   to texto,
                    "autorUid"    to currentUser.uid,
                    "autorNombre" to (currentUser.displayName ?: "Nutriólogo"),
                    "tipo"        to "nota_nutriologo",
                    "creadoEn"    to com.example.nutriia.firebase.firestore.Timestamp.now()
                )
                
                val task = db.collection("usuarios").document(ownerUid)
                    .collection("hijos").document(childId)
                    .collection("consultas").document(docId).set(data)
                    
                if (OfflineManager.hayConexion()) {
                    task.await()
                }

                // El listener en tiempo real actualizará notasConsulta automáticamente
                _ui.value = _ui.value.copy(
                    guardandoNota    = false,
                    mostrarFormaNota = false,
                    exito            = "Nota clínica guardada"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    guardandoNota = false,
                    error         = "No se pudo guardar la nota"
                )
            }
        }
    }

    // ── Entradas de alimentación ──────────────────────────────────────────

    fun guardarEntradaAlimentacion(titulo: String, contenido: String, tipo: String) {
        val currentUser = auth.currentUser ?: return
        if (contenido.isBlank()) return

        viewModelScope.launch {
            _ui.value = _ui.value.copy(guardandoEntradaAlim = true)
            try {
                val docId = db.collection("usuarios").document(_ownerUid)
                    .collection("hijos").document(_childId)
                    .collection("consultas").document().id

                val data = mapOf(
                    "id"          to docId,
                    "titulo"      to titulo.trim(),
                    "texto"       to contenido.trim(),
                    "contenido"   to contenido.trim(),
                    "autorUid"    to currentUser.uid,
                    "autorNombre" to (currentUser.displayName ?: "Nutriólogo"),
                    "tipo"        to tipo,
                    "creadoEn"    to com.example.nutriia.firebase.firestore.Timestamp.now()
                )
                
                val task = db.collection("usuarios").document(_ownerUid)
                    .collection("hijos").document(_childId)
                    .collection("consultas").document(docId).set(data)
                    
                if (OfflineManager.hayConexion()) {
                    task.await()
                }

                // El listener en tiempo real actualizará entradasAlimentacion automáticamente
                _ui.value = _ui.value.copy(
                    guardandoEntradaAlim = false,
                    mostrarFormaAlim     = false,
                    exito                = if (tipo == "receta_nutriologo") "Receta guardada" else "Observación guardada"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    guardandoEntradaAlim = false,
                    error                = "No se pudo guardar"
                )
            }
        }
    }

    // ── Receta personalizada con dual-write a recetas_nutriologo ──────────
    // Guarda en consultas (para el expediente) Y en recetas_nutriologo
    // (para que aparezca en SolidosScreen tab Recetas)

    fun guardarRecetaPersonalizada(
        titulo:        String,
        ingredientes:  String,
        preparacion:   String,
        tipoComida:    TipoComida,
        kcal:          Int
    ) {
        val currentUser = auth.currentUser ?: return
        if (titulo.isBlank() || preparacion.isBlank()) return

        val ingredientesList = ingredientes.split(",", "\n", ";")
            .map { it.trim() }
            .filter { it.isNotBlank() }

        viewModelScope.launch {
            _ui.value = _ui.value.copy(guardandoEntradaAlim = true)
            try {
                val contenidoCompleto = buildString {
                    if (ingredientesList.isNotEmpty()) {
                        appendLine("Ingredientes: ${ingredientesList.joinToString(", ")}")
                        appendLine()
                    }
                    append(preparacion.trim())
                }

                // Generar ID único para ambas colecciones para poder borrar dualmente de forma sencilla
                val docId = db.collection("usuarios").document(_ownerUid)
                    .collection("hijos").document(_childId)
                    .collection("consultas").document().id

                // 1. Guardar en consultas (para el expediente del nutriólogo)
                val consultaData = mapOf(
                    "id"          to docId,
                    "titulo"      to titulo.trim(),
                    "texto"       to contenidoCompleto,
                    "contenido"   to contenidoCompleto,
                    "autorUid"    to currentUser.uid,
                    "autorNombre" to (currentUser.displayName ?: "Nutriólogo"),
                    "tipo"        to "receta_nutriologo",
                    "creadoEn"    to com.example.nutriia.firebase.firestore.Timestamp.now()
                )
                
                val task1 = db.collection("usuarios").document(_ownerUid)
                    .collection("hijos").document(_childId)
                    .collection("consultas").document(docId).set(consultaData)
                    
                if (OfflineManager.hayConexion()) {
                    task1.await()
                }

                // 2. Dual-write: guardar en recetas_nutriologo (para SolidosScreen)
                val recetaData = mapOf(
                    "id"           to docId,
                    "nombre"       to titulo.trim(),
                    "ingredientes" to ingredientesList,
                    "preparacion"  to preparacion.trim(),
                    "kcal"         to kcal,
                    "tipoComida"   to tipoComida.name,
                    "autorUid"     to currentUser.uid,
                    "autorNombre"  to (currentUser.displayName ?: "Nutriólogo"),
                    "edadMeses"    to _ui.value.edadMeses,
                    "creadoEn"     to com.example.nutriia.firebase.firestore.Timestamp.now()
                )
                
                val task2 = db.collection("usuarios").document(_ownerUid)
                    .collection("hijos").document(_childId)
                    .collection("recetas_nutriologo").document(docId).set(recetaData)
                    
                if (OfflineManager.hayConexion()) {
                    task2.await()
                }

                // Los listeners actualizan la UI automáticamente
                _ui.value = _ui.value.copy(
                    guardandoEntradaAlim = false,
                    mostrarFormaAlim     = false,
                    exito                = "Receta personalizada guardada"
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(
                    guardandoEntradaAlim = false,
                    error                = "No se pudo guardar la receta"
                )
            }
        }
    }

    // ── Eliminar receta u observación nutricional ──────────────────────────

    fun eliminarEntradaAlimentacion(id: String, tipo: String, titulo: String) {
        val currentUser = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                // 1. Borrar de consultas
                val task1 = db.collection("usuarios").document(_ownerUid)
                    .collection("hijos").document(_childId)
                    .collection("consultas").document(id).delete()
                    
                if (OfflineManager.hayConexion()) {
                    task1.await()
                }

                // 2. Si es receta, borrar también de recetas_nutriologo
                if (tipo == "receta_nutriologo") {
                    // Intento 2a: Borrar por ID directo (para registros nuevos con IDs enlazados)
                    try {
                        val task2 = db.collection("usuarios").document(_ownerUid)
                            .collection("hijos").document(_childId)
                            .collection("recetas_nutriologo").document(id).delete()
                            
                        if (OfflineManager.hayConexion()) {
                            task2.await()
                        }
                    } catch (e: Exception) {
                        // Ignorar fallas (como PERMISSION_DENIED por inexistencia en registros antiguos)
                    }

                    // Intento 2b: Fallback por query de Nombre y Autor (para limpiar registros antiguos con IDs distintos)
                    try {
                        if (OfflineManager.hayConexion()) {
                            val matchingDocs = db.collection("usuarios").document(_ownerUid)
                                .collection("hijos").document(_childId)
                                .collection("recetas_nutriologo")
                                .whereEqualTo("nombre", titulo.trim())
                                .whereEqualTo("autorUid", currentUser.uid)
                                .get().await()

                            val docsList = (matchingDocs as? com.example.nutriia.firebase.firestore.QuerySnapshot)?.documents ?: emptyList()
                            for (doc in docsList) {
                                doc.reference.delete().await()
                            }
                        }
                    } catch (e: Exception) {
                        // Ignorar fallas
                    }
                }

                _ui.value = _ui.value.copy(exito = "Alimento/Receta eliminado correctamente")
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(error = "No se pudo eliminar el registro")
            }
        }
    }

    // ── Helpers de UI ─────────────────────────────────────────────────────

    fun seleccionarTab(index: Int)     { _ui.value = _ui.value.copy(tabSeleccionado = index) }
    fun mostrarFormaNota()             { _ui.value = _ui.value.copy(mostrarFormaNota = true, exito = null) }
    fun ocultarFormaNota()             { _ui.value = _ui.value.copy(mostrarFormaNota = false) }
    fun mostrarFormaAlim(tipo: String) { _ui.value = _ui.value.copy(mostrarFormaAlim = true, tipoEntradaAlim = tipo, exito = null) }
    fun ocultarFormaAlim()             { _ui.value = _ui.value.copy(mostrarFormaAlim = false) }
    fun limpiarMensajes()              { _ui.value = _ui.value.copy(exito = null, error = null) }

    private fun iniciarListenerCrecimiento(ownerUid: String, childId: String, basePeso: Double, baseTalla: Double) {
        crecimientoListener?.remove()
        crecimientoListener = db.collection("usuarios")
            .document(ownerUid)
            .collection("hijos")
            .document(childId)
            .collection("crecimiento")
            .addSnapshotListener { snap, err ->
                if (err != null) { Log.e("Expediente", "Error listener crecimiento", err); return@addSnapshotListener }
                val lista = snap?.documents?.mapNotNull { doc ->
                    runCatching {
                        MedicionCrecimiento(
                            id        = doc.id,
                            childId   = doc.getString("childId") ?: "",
                            userId    = doc.getString("userId") ?: "",
                            fecha     = doc.getString("fecha") ?: "",
                            pesoKg    = doc.getDouble("pesoKg") ?: 0.0,
                            tallaCm   = doc.getDouble("tallaCm") ?: 0.0,
                            circCefCm = doc.getDouble("circCefCm") ?: 0.0,
                            notas     = doc.getString("notas") ?: "",
                            creadoEn  = doc.getTimestamp("creadoEn")
                        )
                    }.getOrNull()
                } ?: emptyList()

                // Ordenar en memoria por fecha y creadoEn descendente
                val ordenada = lista.sortedWith(
                    compareByDescending<MedicionCrecimiento> { it.fecha }
                        .thenByDescending { it.creadoEn?.seconds ?: 0L }
                        .thenByDescending { it.creadoEn?.nanoseconds ?: 0 }
                )

                // Extraer el último peso y talla (o fallback si no hay mediciones)
                val ult = ordenada.firstOrNull()
                val ultPeso = ult?.pesoKg ?: basePeso
                val ultTalla = ult?.tallaCm ?: baseTalla
                val ultFecha = ult?.fecha ?: ""
                val ultMedicionStr = if (ultFecha.isNotBlank()) "${ultPeso}kg / ${ultTalla}cm — $ultFecha" else ""

                _ui.value = _ui.value.copy(
                    historialCrecimiento = ordenada,
                    weightKg = ultPeso,
                    heightCm = ultTalla,
                    ultimaMedicionCrec = ultMedicionStr
                )
            }
    }

    /**
     * Soporta los formatos:
     *   "dd/MM/yyyy"  → usado en la app original
     *   "yyyy-MM-dd"  → ISO
     *   "MM/dd/yyyy"  → fallback
     */
    private fun calcularEdadMeses(birthDate: String): Int {
        if (birthDate.isBlank()) return 0
        return try {
            val (dia, mes, anio) = if (birthDate.contains("/")) {
                val p = birthDate.split("/").map { it.toInt() }
                if (p[0] > 31) Triple(p[2], p[1], p[0]) else Triple(p[0], p[1], p[2])
            } else {
                val p = birthDate.split("-").map { it.toInt() }
                Triple(p[2], p[1], p[0])
            }
            val currentYear = 2026
            val currentMonth = 8
            val anios = currentYear - anio
            val meses = currentMonth - mes
            (anios * 12 + meses).coerceAtLeast(0)
        } catch (_: Exception) { 0 }
    }
}
