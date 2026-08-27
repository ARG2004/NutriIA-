package com.example.nutriia.auth

import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.crecimiento.Sexo
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.ginecologo.GinecologoRepository
import com.example.nutriia.offline.OfflineManager
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.ui.theme.ChildProfile
import com.example.nutriia.utils.FechaUtils
import com.example.nutriia.platform.generateUUID
import com.example.nutriia.platform.currentTimeMillis
import com.example.nutriia.firebase.auth.FirebaseAuth
import com.example.nutriia.firebase.auth.FirebaseUser
import com.example.nutriia.firebase.firestore.FirebaseFirestore
import com.example.nutriia.firebase.firestore.await

sealed class ResultadoAuth {
    data class Exito(val uid: String, val rol: String) : ResultadoAuth()
    data class Error(val mensaje: String)              : ResultadoAuth()
}

class RepositorioLogin {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val rolCache = mutableMapOf<String, String>()

    private fun guardarRolCache(uid: String, rol: String) {
        rolCache[uid] = rol
    }

    private fun obtenerRolCache(uid: String): String? {
        return rolCache[uid]
    }

    suspend fun login(email: String, contrasena: String): ResultadoAuth {
        if (email.isBlank() || contrasena.isBlank())
            return ResultadoAuth.Error("Completa todos los campos")

        if (!OfflineManager.hayConexion()) {
            val usuario = auth.currentUser
                ?: return ResultadoAuth.Error("Sin conexión. Inicia sesión online al menos una vez")
            val rol = obtenerRolCache(usuario.uid) ?: "padre"
            return ResultadoAuth.Exito(usuario.uid, rol)
        }

        return try {
            val resultado = auth.signInWithEmailAndPassword(email.trim(), contrasena).await()
            val usuario   = resultado.user
                ?: return ResultadoAuth.Error("No se pudo obtener el usuario")

            val rol   = obtenerRol(usuario.uid)
            if (rol == "nutriologo") {
                try {
                    val userDoc = db.collection("usuarios").document(usuario.uid).get().await()
                    val uNombre = userDoc.getString("nombre") ?: ""
                    val uEspecialidad = userDoc.getString("especialidad") ?: "Nutrición Pediátrica"
                    val uCedula = userDoc.getString("cedula") ?: ""
                    val uEmail = userDoc.getString("email") ?: usuario.email ?: ""
                    com.example.nutriia.vinculacion.VinculacionRepository().publicarPerfilNutriologo(
                        nombre = uNombre,
                        especialidad = uEspecialidad,
                        cedula = uCedula,
                        email = uEmail
                    )
                } catch (_: Exception) {}
            }
            guardarRolCache(usuario.uid, rol)
            SessionManager.guardarSesion(usuario.uid)
            ResultadoAuth.Exito(usuario.uid, rol)

        } catch (e: Exception) {
            ResultadoAuth.Error(traducirError(e.message))
        }
    }

    suspend fun registrarPadre(
        email: String,
        contrasena: String,
        nombre: String,
        telefono: String,
        codigoNutriologo: String,
        nombreHijo: String
    ): ResultadoAuth {
        if (email.isBlank() || contrasena.isBlank())
            return ResultadoAuth.Error("Completa todos los campos")

        if (!OfflineManager.hayConexion())
            return ResultadoAuth.Error("Necesitas conexión para registrarte")

        return try {
            val resultado = auth.createUserWithEmailAndPassword(email.trim(), contrasena).await()
            val usuario   = resultado.user
                ?: return ResultadoAuth.Error("No se pudo crear el usuario")

            val datos = mutableMapOf<String, Any>(
                "email"             to email.trim(),
                "nombre"            to nombre,
                "telefono"          to telefono,
                "nombreHijo"        to nombreHijo,
                "rol"               to "padre",
                "modoAccesibilidad" to AccessibilityMode.NORMAL.name,
                "creadoEn"          to FechaUtils.fechaHoraActual(),
                "fechaCreacion"     to FechaUtils.fechaActual(),
                "horaCreacion"      to FechaUtils.horaActual()
            )
            if (codigoNutriologo.isNotBlank()) datos["codigoNutriologo"] = codigoNutriologo

            db.collection("usuarios").document(usuario.uid).set(datos).await()

            guardarRolCache(usuario.uid, "padre")
            SessionManager.guardarSesion(usuario.uid)
            ResultadoAuth.Exito(usuario.uid, "padre")

        } catch (e: Exception) {
            ResultadoAuth.Error(traducirError(e.message))
        }
    }

    suspend fun registrarMamaPrimeriza(
        email: String,
        contrasena: String,
        nombre: String,
        telefono: String,
        semanas: Int
    ): ResultadoAuth {
        if (email.isBlank() || contrasena.isBlank())
            return ResultadoAuth.Error("Completa todos los campos")

        if (!OfflineManager.hayConexion())
            return ResultadoAuth.Error("Necesitas conexión para registrarte")

        return try {
            val resultado = auth.createUserWithEmailAndPassword(email.trim(), contrasena).await()
            val usuario   = resultado.user
                ?: return ResultadoAuth.Error("No se pudo crear el usuario")

            val datos = mapOf(
                "email"             to email.trim(),
                "nombre"            to nombre,
                "telefono"          to telefono,
                "semanasEmbarazo"   to semanas,
                "rol"               to "mama_primeriza",
                "modoAccesibilidad" to AccessibilityMode.NORMAL.name,
                "creadoEn"          to FechaUtils.fechaHoraActual(),
                "fechaCreacion"     to FechaUtils.fechaActual(),
                "horaCreacion"      to FechaUtils.horaActual()
            )

            db.collection("usuarios").document(usuario.uid).set(datos).await()

            guardarRolCache(usuario.uid, "mama_primeriza")
            SessionManager.guardarSesion(usuario.uid)
            ResultadoAuth.Exito(usuario.uid, "mama_primeriza")

        } catch (e: Exception) {
            ResultadoAuth.Error(traducirError(e.message))
        }
    }

    suspend fun esCedulaRegistrada(cedula: String): Boolean {
        val cedulaLimpia = cedula.filter(Char::isDigit).trim()
        if (cedulaLimpia.isBlank()) return false
        return try {
            val queryUsuarios = db.collection("usuarios")
                .whereEqualTo("cedula", cedulaLimpia)
                .get()
                .await()
            if (!queryUsuarios.isEmpty) return true

            val queryUsuariosOriginal = db.collection("usuarios")
                .whereEqualTo("cedula", cedula.trim())
                .get()
                .await()
            if (!queryUsuariosOriginal.isEmpty) return true

            val queryNutriologos = db.collection("nutriologos_publicos")
                .whereEqualTo("cedula", cedulaLimpia)
                .get()
                .await()
            if (!queryNutriologos.isEmpty) return true

            val queryGinecologos = db.collection("ginecologos_publicos")
                .whereEqualTo("cedula", cedulaLimpia)
                .get()
                .await()
            !queryGinecologos.isEmpty
        } catch (e: Exception) {
            false
        }
    }

    suspend fun registrarNutriologo(
        email: String,
        contrasena: String,
        nombre: String,
        telefono: String,
        especialidad: String,
        cedula: String,
        consentimientoCedula: Boolean = true,
        nombreTitularCedula: String = "",
        profesionCedula: String = ""
    ): ResultadoAuth {
        if (email.isBlank() || contrasena.isBlank())
            return ResultadoAuth.Error("Completa todos los campos")

        if (!OfflineManager.hayConexion())
            return ResultadoAuth.Error("Necesitas conexión para registrarte")

        if (esCedulaRegistrada(cedula))
            return ResultadoAuth.Error("Esta cédula profesional ya pertenece a otro especialista registrado en NutrIA.")

        return try {
            val resultado = auth.createUserWithEmailAndPassword(email.trim(), contrasena).await()
            val usuario   = resultado.user
                ?: return ResultadoAuth.Error("No se pudo crear el usuario")

            val datosUsuario = mutableMapOf<String, Any>(
                "email"                 to email.trim(),
                "nombre"                to nombre,
                "telefono"              to telefono,
                "especialidad"          to especialidad,
                "cedula"                to cedula,
                "cedulaValida"          to true,
                "nombreTitularCedula"   to nombreTitularCedula,
                "profesionCedula"       to profesionCedula,
                "consentimientoCedula" to consentimientoCedula,
                "fechaConsentimiento"   to com.example.nutriia.shared.Timestamp.now(),
                "needsReverification"   to false,
                "rol"                   to "nutriologo",
                "modoAccesibilidad"     to AccessibilityMode.NORMAL.name,
                "creadoEn"              to FechaUtils.fechaHoraActual(),
                "fechaCreacion"         to FechaUtils.fechaActual(),
                "horaCreacion"          to FechaUtils.horaActual()
            )

            db.collection("usuarios").document(usuario.uid).set(datosUsuario).await()

            // Publicar perfil público para que aparezca en el directorio de especialistas
            try {
                com.example.nutriia.vinculacion.VinculacionRepository().publicarPerfilNutriologo(
                    nombre       = nombre,
                    especialidad = especialidad,
                    cedula       = cedula,
                    email        = email
                )
            } catch (_: Exception) {}

            guardarRolCache(usuario.uid, "nutriologo")
            SessionManager.guardarSesion(usuario.uid)
            ResultadoAuth.Exito(usuario.uid, "nutriologo")

        } catch (e: Exception) {
            ResultadoAuth.Error(traducirError(e.message))
        }
    }

    // ── Registro de Ginecólogo ───────────────────────────────────────────────
    suspend fun registrarGinecologo(
        email: String,
        contrasena: String,
        nombre: String,
        telefono: String,
        especialidad: String,
        cedula: String,
        consentimientoCedula: Boolean = true,
        nombreTitularCedula: String = "",
        profesionCedula: String = ""
    ): ResultadoAuth {
        if (email.isBlank() || contrasena.isBlank())
            return ResultadoAuth.Error("Completa todos los campos")

        if (!OfflineManager.hayConexion())
            return ResultadoAuth.Error("Necesitas conexión para registrarte")

        if (esCedulaRegistrada(cedula))
            return ResultadoAuth.Error("Esta cédula profesional ya pertenece a otro especialista registrado en NutrIA.")

        return try {
            val resultado = auth.createUserWithEmailAndPassword(email.trim(), contrasena).await()
            val usuario   = resultado.user
                ?: return ResultadoAuth.Error("No se pudo crear el usuario")

            val datosUsuario = mutableMapOf<String, Any>(
                "email"                 to email.trim(),
                "nombre"                to nombre,
                "telefono"              to telefono,
                "especialidad"          to especialidad,
                "cedula"                to cedula,
                "cedulaValida"          to true,
                "nombreTitularCedula"   to nombreTitularCedula,
                "profesionCedula"       to profesionCedula,
                "consentimientoCedula" to consentimientoCedula,
                "fechaConsentimiento"   to com.example.nutriia.shared.Timestamp.now(),
                "needsReverification"   to false,
                "rol"                   to "ginecologo",
                "modoAccesibilidad"     to AccessibilityMode.NORMAL.name,
                "creadoEn"              to FechaUtils.fechaHoraActual(),
                "fechaCreacion"         to FechaUtils.fechaActual(),
                "horaCreacion"          to FechaUtils.horaActual()
            )

            db.collection("usuarios").document(usuario.uid).set(datosUsuario).await()

            // DECISIÓN: A diferencia del nutriólogo, aquí publicamos el perfil público de inmediato
            GinecologoRepository().publicarPerfilGinecologo(
                nombre       = nombre,
                especialidad = especialidad,
                cedula       = cedula,
                email        = email
            )

            guardarRolCache(usuario.uid, "ginecologo")
            SessionManager.guardarSesion(usuario.uid)
            ResultadoAuth.Exito(usuario.uid, "ginecologo")

        } catch (e: Exception) {
            ResultadoAuth.Error(traducirError(e.message))
        }
    }

    // ── Derechos ARCO: Eliminación de Datos de Verificación de Cédula ───────
    suspend fun eliminarDatosVerificacionCedula(uid: String): Boolean {
        return try {
            db.collection("usuarios").document(uid).update(
                mapOf(
                    "cedulaValida"          to com.example.nutriia.firebase.firestore.FieldValue.delete,
                    "nombreTitularCedula"   to com.example.nutriia.firebase.firestore.FieldValue.delete,
                    "profesionCedula"       to com.example.nutriia.firebase.firestore.FieldValue.delete,
                    "consentimientoCedula" to com.example.nutriia.firebase.firestore.FieldValue.delete,
                    "fechaConsentimiento"   to com.example.nutriia.firebase.firestore.FieldValue.delete,
                    "needsReverification"   to true
                )
            ).await()
            true
        } catch (e: Throwable) {
            com.example.nutriia.platform.Log.e("RepositorioLogin", "Error al eliminar datos de verificación ARCO: ${e.message}")
            false
        }
    }

    fun verificarSesionActiva(): ResultadoAuth? {
        val usuario = auth.currentUser ?: return null
        val rol = obtenerRolCache(usuario.uid) ?: "padre"
        return ResultadoAuth.Exito(usuario.uid, rol)
    }

    suspend fun guardarModoAccesibilidad(uid: String, modo: AccessibilityMode): Boolean {
        return try {
            db.collection("usuarios").document(uid)
                .update("modoAccesibilidad", modo.name).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun cargarModoAccesibilidad(uid: String): AccessibilityMode {
        return try {
            val snap  = db.collection("usuarios").document(uid).get().await()
            val saved = snap.getString("modoAccesibilidad") ?: AccessibilityMode.NORMAL.name
            runCatching { AccessibilityMode.valueOf(saved) }.getOrDefault(AccessibilityMode.NORMAL)
        } catch (e: Exception) { AccessibilityMode.NORMAL }
    }

    suspend fun guardarHijo(uid: String, child: ChildProfile): Boolean {
        return try {
            val childId = child.id.ifBlank { generateUUID() }
            val datosHijo = mapOf(
                "id"               to childId,
                "name"             to child.name,
                "nombre"           to child.name,
                "nombreHijo"       to child.name,
                "birthDate"        to child.birthDate,
                "fechaNacimiento"  to child.birthDate,
                "weightKg"         to child.weightKg,
                "peso"             to child.weightKg,
                "heightCm"         to child.heightCm,
                "talla"            to child.heightCm,
                "hasAllergies"     to child.hasAllergies,
                "allergiesDetail"  to child.allergiesDetail,
                "hasConditions"    to child.hasConditions,
                "conditionsDetail" to child.conditionsDetail,
                "sexo"             to (child.sexo?.name ?: ""),
                "nivelIngreso"     to child.nivelIngreso.name,
                "region"           to child.region.name,
                "creadoEn"         to FechaUtils.fechaHoraActual(),
                "fechaCreacion"    to FechaUtils.fechaActual(),
                "horaCreacion"     to FechaUtils.horaActual()
            )
            db.collection("usuarios").document(uid)
                .collection("hijos").document(childId)
                .set(datosHijo).await()

            // Sincronizar nombreHijo en el documento principal del usuario
            try {
                db.collection("usuarios").document(uid).update("nombreHijo", child.name).await()
            } catch (_: Exception) {}

            // Crear registro inicial en subcolección de crecimiento si se ingresó peso o talla
            val pesoNum = child.weightKg.replace(",", ".").toDoubleOrNull() ?: 0.0
            val tallaNum = child.heightCm.replace(",", ".").toDoubleOrNull() ?: 0.0
            if (pesoNum > 0.0 || tallaNum > 0.0) {
                try {
                    val crecCol = db.collection("usuarios").document(uid)
                        .collection("hijos").document(childId).collection("crecimiento")
                    val existing = crecCol.get().await().documents
                    if (existing.isEmpty()) {
                        val crecId = generateUUID()
                        val datosCrec = mapOf(
                            "id" to crecId,
                            "childId" to childId,
                            "userId" to uid,
                            "fecha" to FechaUtils.fechaActual(),
                            "pesoKg" to pesoNum,
                            "tallaCm" to tallaNum,
                            "circCefCm" to 0.0,
                            "notas" to "Registro inicial de nacimiento / perfil",
                            "notes" to "Initial record",
                            "creadoEnMillis" to currentTimeMillis(),
                            "fechaCreacion" to FechaUtils.fechaActual(),
                            "horaCreacion" to FechaUtils.horaActual()
                        )
                        crecCol.document(crecId).set(datosCrec).await()
                    }
                } catch (_: Exception) {}
            }

            true
        } catch (e: Exception) { false }
    }

    suspend fun cargarHijos(uid: String): List<ChildProfile> {
        return try {
            val parentDoc = try {
                db.collection("usuarios").document(uid).get().await()
            } catch (_: Exception) { null }
            val fallbackChildName = parentDoc?.getString("nombreHijo")?.takeIf { it.isNotBlank() } ?: "Mi Pequeño/a"

            val docs = db.collection("usuarios").document(uid)
                .collection("hijos").get().await().documents

            val hijosList = docs.mapNotNull { doc ->
                val name = doc.getString("name")
                    ?: doc.getString("nombre")
                    ?: doc.getString("nombreHijo")
                    ?: doc.getString("childName")
                    ?: fallbackChildName
                
                val weight = doc.getString("weightKg") 
                    ?: doc.getDouble("weightKg")?.toString() 
                    ?: doc.getString("peso") 
                    ?: doc.getDouble("peso")?.toString() 
                    ?: ""
                
                val height = doc.getString("heightCm") 
                    ?: doc.getDouble("heightCm")?.toString() 
                    ?: doc.getString("talla") 
                    ?: doc.getDouble("talla")?.toString() 
                    ?: ""
                
                val birthDate = doc.getString("birthDate") 
                    ?: doc.getString("fechaNacimiento") 
                    ?: ""

                ChildProfile(
                    id               = doc.getString("id") ?: doc.id,
                    name             = name,
                    birthDate        = birthDate,
                    weightKg         = weight,
                    heightCm         = height,
                    hasAllergies     = doc.getBoolean("hasAllergies") ?: (doc.getBoolean("tieneAlergias") ?: false),
                    allergiesDetail  = doc.getString("allergiesDetail") ?: (doc.getString("alergias") ?: ""),
                    hasConditions    = doc.getBoolean("hasConditions") ?: (doc.getBoolean("tieneCondiciones") ?: false),
                    conditionsDetail = doc.getString("conditionsDetail") ?: (doc.getString("condiciones") ?: ""),
                    sexo             = doc.getString("sexo")
                        ?.takeIf { it.isNotBlank() }
                        ?.let { runCatching { Sexo.valueOf(it) }.getOrNull() },
                    nivelIngreso     = doc.getString("nivelIngreso")
                        ?.let { runCatching { NivelIngreso.valueOf(it) }.getOrDefault(NivelIngreso.BASICO) }
                        ?: NivelIngreso.BASICO,
                    region           = doc.getString("region")
                        ?.let { runCatching { RegionMexico.valueOf(it) }.getOrDefault(RegionMexico.CENTRO) }
                        ?: RegionMexico.CENTRO
                )
            }

            if (hijosList.isNotEmpty()) {
                hijosList
            } else if (parentDoc != null && !parentDoc.getString("nombreHijo").isNullOrBlank()) {
                // Fallback para usuarios antiguos que tienen nombreHijo en el doc principal
                val nombreHijo = parentDoc.getString("nombreHijo")!!.trim()
                listOf(
                    ChildProfile(
                        id = generateUUID(),
                        name = nombreHijo,
                        birthDate = parentDoc.getString("birthDate") ?: parentDoc.getString("fechaNacimiento") ?: "",
                        weightKg = parentDoc.getString("weightKg") ?: parentDoc.getString("peso") ?: "",
                        heightCm = parentDoc.getString("heightCm") ?: parentDoc.getString("talla") ?: "",
                        nivelIngreso = NivelIngreso.BASICO,
                        region = RegionMexico.CENTRO
                    )
                )
            } else {
                emptyList()
            }
        } catch (e: Exception) { 
            com.example.nutriia.platform.Log.e("LoginRepository", "Error cargando hijos de $uid: ${e.message}")
            emptyList() 
        }
    }

    suspend fun guardarPerfilEmbarazo(uid: String, perfil: PerfilEmbarazo): Boolean {
        return try {
            db.collection("usuarios").document(uid)
                .collection("perfilEmbarazo").document("unico")
                .set(perfil.toMap()).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun cargarPerfilEmbarazo(uid: String): PerfilEmbarazo? {
        return try {
            val snap = db.collection("usuarios").document(uid)
                .collection("perfilEmbarazo").document("unico").get().await()
            if (snap.exists()) PerfilEmbarazo.fromMap(snap.data ?: emptyMap()) else null
        } catch (e: Exception) { null }
    }

    suspend fun obtenerRol(uid: String): String {
        return try {
            val rol = db.collection("usuarios").document(uid).get().await()
                .getString("rol") ?: "padre"
            guardarRolCache(uid, rol)
            rol
        } catch (e: Exception) {
            obtenerRolCache(uid) ?: "padre"
        }
    }

    fun obtenerRolCacheSync(uid: String): String? = obtenerRolCache(uid)

    suspend fun recuperarContrasena(email: String): Boolean {
        if (email.isBlank()) return false
        return try {
            auth.sendPasswordResetEmail(email.trim())
            true
        } catch (e: Exception) { false }
    }

    fun obtenerUsuarioActual(): FirebaseUser? = auth.currentUser

    suspend fun cerrarSesionCompleta() {
        auth.signOut()
        SessionManager.limpiarSesion()
        rolCache.clear()
        if (OfflineManager.hayConexion()) {
            try {
                db.clearPersistence().await()
            } catch (e: Exception) { }
        }
    }

    fun cerrarSesionRapida() {
        // No hace nada con Auth ni Firestore por diseño para permitir re-login con huella
    }

    suspend fun cerrarSesion() {
        SessionManager.limpiarSesion()
        rolCache.clear()
    }

    fun cerrarSesionBiometrica() {
        SessionManager.limpiarSesion()
    }

    suspend fun actualizarContrasena(contrasenaActual: String, nuevaContrasena: String): ResultadoAuth {
        if (contrasenaActual.isBlank() || nuevaContrasena.isBlank())
            return ResultadoAuth.Error("Completa todos los campos")

        if (nuevaContrasena.length < 6)
            return ResultadoAuth.Error("La nueva contraseña debe tener al menos 6 caracteres")

        val usuario = auth.currentUser
            ?: return ResultadoAuth.Error("No hay una sesión activa")

        val email = usuario.email ?: return ResultadoAuth.Error("No se pudo obtener el correo de la cuenta")

        return try {
            val credencial = com.example.nutriia.firebase.auth.EmailAuthProvider.credential(email, contrasenaActual)
            usuario.reauthenticate(credencial).await()
            usuario.updatePassword(nuevaContrasena).await()
            ResultadoAuth.Exito(usuario.uid, "contrasena_actualizada")
        } catch (e: Exception) {
            ResultadoAuth.Error(traducirError(e.message))
        }
    }

    suspend fun eliminarCuenta(contrasenaActual: String): ResultadoAuth {
        if (contrasenaActual.isBlank())
            return ResultadoAuth.Error("Ingresa tu contraseña actual para confirmar")

        val usuario = auth.currentUser
            ?: return ResultadoAuth.Error("No hay una sesión activa")

        val email = usuario.email ?: return ResultadoAuth.Error("No se pudo obtener el correo de la cuenta")

        return try {
            val credencial = com.example.nutriia.firebase.auth.EmailAuthProvider.credential(email, contrasenaActual)
            usuario.reauthenticate(credencial).await()

            val uid = usuario.uid
            try {
                db.collection("usuarios").document(uid).delete().await()
            } catch (_: Exception) {}

            usuario.delete().await()

            SessionManager.limpiarSesion()
            rolCache.clear()

            ResultadoAuth.Exito(uid, "cuenta_eliminada")
        } catch (e: Exception) {
            ResultadoAuth.Error(traducirError(e.message))
        }
    }

    suspend fun decrementarIntentoIa(uid: String, intentosRestantes: Int): Boolean {
        return try {
            val nuevoValor = if (intentosRestantes > 0) intentosRestantes - 1 else 0
            db.collection("usuarios").document(uid).update(
                mapOf("intentosIaDisponibles" to nuevoValor)
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun resetearIntentosDiarios(uid: String, nuevoReset: Long): Boolean {
        return try {
            db.collection("usuarios").document(uid).update(
                mapOf(
                    "intentosIaDisponibles" to 3,
                    "ultimoResetIa" to nuevoReset
                )
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun activarSuscripcionIa(uid: String): Boolean {
        return try {
            // +30 días en milisegundos
            val treintaDiasMilis = 30L * 24 * 60 * 60 * 1000
            val vigenteHasta = currentTimeMillis() + treintaDiasMilis
            db.collection("usuarios").document(uid).update(
                mapOf("suscripcionIaVigenteHasta" to vigenteHasta)
            ).await()
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun traducirError(mensaje: String?): String {
        val msg = mensaje?.lowercase() ?: return "Correo o contraseña incorrectos"
        return when {
            "already in use" in msg || "email-already-in-use" in msg || "email_already_exists" in msg ->
                "Este correo ya ha sido registrado"
            "password is invalid" in msg || "credential is incorrect" in msg || "auth credential" in msg ||
                    "wrong-password" in msg || "invalid-credential" in msg || "invalid_login_credentials" in msg ||
                    "no user record" in msg || "user-not-found" in msg ->
                "Correo o contraseña incorrectos"
            "email address is bad" in msg || "invalid-email" in msg ->
                "Correo electrónico inválido"
            "too many requests" in msg ->
                "Demasiados intentos. Intenta más tarde"
            "network error" in msg || "unable to resolve host" in msg || "timeout" in msg ->
                "Sin conexión a internet"
            "weak-password" in msg ->
                "La contraseña debe tener al menos 6 caracteres"
            "user disabled" in msg ->
                "Esta cuenta ha sido deshabilitada"
            else -> "Correo o contraseña incorrectos"
        }
    }
}
