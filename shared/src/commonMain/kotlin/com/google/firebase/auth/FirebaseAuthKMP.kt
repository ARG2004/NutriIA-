package com.google.firebase.auth

data class AuthResult(
    val user: FirebaseUser? = FirebaseUser()
)

data class FirebaseUser(
    val uid: String = "user_default_uid",
    val email: String? = "usuario@nutriia.com",
    val displayName: String = "Nutriólogo",
    val isEmailVerified: Boolean = true
) {
    suspend fun sendEmailVerification() {}
    suspend fun updatePassword(newPass: String) {}
    suspend fun reauthenticate(credential: Any?) {}
    suspend fun delete() {}
}

class FirebaseAuth {
    var currentUser: FirebaseUser? = FirebaseUser()

    suspend fun signInWithEmailAndPassword(email: String, pass: String): AuthResult = AuthResult()
    suspend fun createUserWithEmailAndPassword(email: String, pass: String): AuthResult = AuthResult()
    suspend fun sendPasswordResetEmail(email: String, settings: Any? = null): Any = Any()
    fun signOut() { currentUser = null }

    companion object {
        private val instance = FirebaseAuth()
        fun getInstance(): FirebaseAuth = instance
    }
}

object EmailAuthProvider {
    fun getCredential(email: String, pass: String): Any = Any()
}
