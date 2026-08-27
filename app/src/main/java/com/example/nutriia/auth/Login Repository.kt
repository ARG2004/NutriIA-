package com.example.nutriia.auth

import android.content.Context
import android.content.SharedPreferences
import com.example.nutriia.accesibilidad.AccessibilityMode
import com.example.nutriia.crecimiento.Sexo
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.ginecologo.GinecologoRepository
import com.example.nutriia.offline.OfflineManager
import com.example.nutriia.sueldo.NivelIngreso
import com.example.nutriia.sueldo.RegionMexico
import com.example.nutriia.ui.theme.ChildProfile
import com.example.nutriia.utils.FechaUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID

sealed class ResultadoAuth {
    data class Exito(val uid: String, val rol: String) : ResultadoAuth()
    data class Error(val mensaje: String)              : ResultadoAuth()
}

class RepositorioLogin(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    private val rolCache: SharedPreferences =
        context.getSharedPreferences("nutriia_rol_cache", Context.MODE_PRIVATE)

    private fun guardarRolCache(uid: String, rol: String) {
        rolCache.edit().putString("uid", uid).putString("rol", rol).apply()
    }

    private fun obtenerRolCache(uid: String): String? {
        return if (rolCache.getString("uid", null) == uid)
            rolCache.getString("rol", null)
        else null
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
            SessionManager.guardarSesion(context, usuario.uid)
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
            SessionManager.guardarSesion(context, usuario.uid)
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
            SessionManager.guardarSesion(context, usuario.uid)
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
                "fechaConsentimiento"   to com.google.firebase.Timestamp.now(),
                "needsReverification"   to false,
                "rol"                   to "nutriologo",
                "modoAccesibilidad"     to AccessibilityMode.NORMAL.name,
                "creadoEn"              to FechaUtils.fechaHoraActual(),
                "fechaCreacion"         to FechaUtils.fechaActual(),
                "horaCreacion"          to FechaUtils.horaActual()
            )

            db.collection("usuarios").document(usuario.uid).set(datosUsuario).await()

            guardarRolCache(usuario.uid, "nutriologo")
            SessionManager.guardarSesion(context, usuario.uid)
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
                "fechaConsentimiento"   to com.google.firebase.Timestamp.now(),
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
            SessionManager.guardarSesion(context, usuario.uid)
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
                    "cedulaValida"          to com.google.firebase.firestore.FieldValue.delete(),
                    "nombreTitularCedula"   to com.google.firebase.firestore.FieldValue.delete(),
                    "profesionCedula"       to com.google.firebase.firestore.FieldValue.delete(),
                    "consentimientoCedula" to com.google.firebase.firestore.FieldValue.delete(),
                    "fechaConsentimiento"   to com.google.firebase.firestore.FieldValue.delete(),
                    "needsReverification"   to true
                )
            ).await()
            true
        } catch (e: java.lang.Exception) {
            android.util.Log.e("RepositorioLogin", "Error al eliminar datos de verificación ARCO: ${e.message}")
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
            val childId = child.id.ifBlank { UUID.randomUUID().toString() }
            db.collection("usuarios").document(uid)
                .collection("hijos").document(childId)
                .set(mapOf(
                    "id"               to childId,
                    "name"             to child.name,
                    "birthDate"        to child.birthDate,
                    "weightKg"         to child.weightKg,
                    "heightCm"         to child.heightCm,
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
                )).await()
            true
        } catch (e: Exception) { false }
    }

    suspend fun cargarHijos(uid: String): List<ChildProfile> {
        return try {
            db.collection("usuarios").document(uid)
                .collection("hijos").get().await()
                .documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    ChildProfile(
                        id               = doc.getString("id") ?: doc.id,
                        name             = name,
                        birthDate        = doc.getString("birthDate") ?: "",
                        weightKg         = doc.getString("weightKg") ?: "",
                        heightCm         = doc.getString("heightCm") ?: "",
                        hasAllergies     = doc.getBoolean("hasAllergies") ?: false,
                        allergiesDetail  = doc.getString("allergiesDetail") ?: "",
                        hasConditions    = doc.getBoolean("hasConditions") ?: false,
                        conditionsDetail = doc.getString("conditionsDetail") ?: "",
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
        } catch (e: Exception) { emptyList() }
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
            auth.sendPasswordResetEmail(email.trim()).await()
            true
        } catch (e: Exception) { false }
    }

    fun obtenerUsuarioActual(): FirebaseUser? = auth.currentUser

    suspend fun cerrarSesionCompleta() {
        auth.signOut()
        SessionManager.limpiarSesion(context)
        rolCache.edit().clear().apply()
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
        SessionManager.limpiarSesion(context)
        rolCache.edit().clear().apply()
    }

    fun cerrarSesionBiometrica(context: Context) {
        SessionManager.limpiarSesion(context)
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
            val credencial = com.google.firebase.auth.EmailAuthProvider.getCredential(email, contrasenaActual)
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
            val credencial = com.google.firebase.auth.EmailAuthProvider.getCredential(email, contrasenaActual)
            usuario.reauthenticate(credencial).await()

            val uid = usuario.uid
            try {
                db.collection("usuarios").document(uid).delete().await()
            } catch (_: Exception) {}

            usuario.delete().await()

            SessionManager.limpiarSesion(context)
            rolCache.edit().clear().apply()

            ResultadoAuth.Exito(uid, "cuenta_eliminada")
        } catch (e: Exception) {
            ResultadoAuth.Error(traducirError(e.message))
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
    suspend fun activarSuscripcionIa(uid: String): Boolean {
        return try {
            val unMesMillis = 30L * 24 * 60 * 60 * 1000
            val fechaVencimiento = System.currentTimeMillis() + unMesMillis
            db.collection("usuarios").document(uid).update(
                "suscripcionIaVigenteHasta", fechaVencimiento,
                "intentosIaDisponibles", 9999
            ).await()
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun decrementarIntentoIa(uid: String) {
        try {
            val doc = db.collection("usuarios").document(uid).get().await()
            val intentos = doc.getLong("intentosIaDisponibles") ?: 0L
            if (intentos > 0 && intentos < 9999L) {
                db.collection("usuarios").document(uid).update(
                    "intentosIaDisponibles", intentos - 1
                ).await()
            }
        } catch (_: Exception) {}
    }

    suspend fun resetearIntentosDiarios(uid: String, nuevaFechaReset: Long) {
        try {
            db.collection("usuarios").document(uid).update(
                "intentosIaDisponibles", 3,
                "ultimoResetIa", nuevaFechaReset
            ).await()
        } catch (_: Exception) {}
    }
}
