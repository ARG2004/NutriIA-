package dev.gitlive.firebase.auth

class FirebaseAuth {
    companion object {
        fun getInstance(): FirebaseAuth = FirebaseAuth()
    }
    val currentUser: FirebaseUser? get() = FirebaseUser()
    suspend fun signInWithEmailAndPassword(email: String, pass: String): AuthResult = AuthResult()
    suspend fun createUserWithEmailAndPassword(email: String, pass: String): AuthResult = AuthResult()
    suspend fun sendPasswordResetEmail(email: String) {}
    fun signOut() {}
}

class FirebaseUser(
    val uid: String = "user_123",
    val email: String? = "usuario@nutriia.com",
    val displayName: String? = "Usuario Demo"
) {
    suspend fun reload() {}
    suspend fun updatePassword(pass: String) {}
    suspend fun delete() {}
    suspend fun reauthenticate(cred: Any?) {}
}

class AuthResult(val user: FirebaseUser? = FirebaseUser())

object EmailAuthProvider {
    fun credential(email: String, pass: String): Any = Unit
    fun getCredential(email: String, pass: String): Any = Unit
}
