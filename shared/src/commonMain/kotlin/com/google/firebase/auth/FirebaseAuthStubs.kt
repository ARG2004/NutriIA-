package com.google.firebase.auth

import android.net.Uri
import com.google.android.gms.tasks.Task

open class AuthCredential

object EmailAuthProvider {
    const val PROVIDER_ID = "password"
    fun getCredential(email: String, password: String): AuthCredential = AuthCredential()
}

class FirebaseUser(
    val uid: String = "ios_user_default",
    val email: String? = "usuario@nutriia.com",
    val displayName: String? = "Usuario NutriIA",
    val photoUrl: Uri? = null
) {
    fun isEmailVerified(): Boolean = true
    fun reload(): Task<Unit> = Task()
    fun reauthenticate(credential: AuthCredential): Task<Unit> = Task()
    fun updatePassword(newPassword: String): Task<Unit> = Task()
    fun delete(): Task<Unit> = Task()
}

class AuthResult(
    val user: FirebaseUser? = FirebaseUser()
)

class FirebaseAuth private constructor() {
    var currentUser: FirebaseUser? = FirebaseUser()

    fun interface AuthStateListener {
        fun onAuthStateChanged(auth: FirebaseAuth)
    }

    private val authStateListeners = mutableListOf<AuthStateListener>()

    fun addAuthStateListener(listener: AuthStateListener) {
        authStateListeners.add(listener)
        listener.onAuthStateChanged(this)
    }

    fun removeAuthStateListener(listener: AuthStateListener) {
        authStateListeners.remove(listener)
    }

    fun signInWithEmailAndPassword(email: String, password: String): Task<AuthResult> {
        currentUser = FirebaseUser(email = email)
        return Task()
    }

    fun createUserWithEmailAndPassword(email: String, password: String): Task<AuthResult> {
        currentUser = FirebaseUser(email = email)
        return Task()
    }

    fun sendPasswordResetEmail(email: String): Task<Unit> = Task()

    fun signOut() {
        currentUser = null
    }

    companion object {
        private val instance = FirebaseAuth()
        fun getInstance(): FirebaseAuth = instance
    }
}
