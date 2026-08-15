package com.google.firebase.auth

import android.net.Uri

class FirebaseUser(
    val uid: String = "ios_user_default",
    val email: String? = "usuario@nutriia.com",
    val displayName: String? = "Usuario NutriIA",
    val photoUrl: Uri? = null
) {
    fun isEmailVerified(): Boolean = true
    fun reload(): Any? = null
}

class AuthResult(
    val user: FirebaseUser? = FirebaseUser()
)

class FirebaseAuth private constructor() {
    var currentUser: FirebaseUser? = FirebaseUser()

    fun signInWithEmailAndPassword(email: String, password: String): Task<AuthResult> {
        currentUser = FirebaseUser(email = email)
        return Task(AuthResult(currentUser))
    }

    fun createUserWithEmailAndPassword(email: String, password: String): Task<AuthResult> {
        currentUser = FirebaseUser(email = email)
        return Task(AuthResult(currentUser))
    }

    fun sendPasswordResetEmail(email: String): Task<Unit> {
        return Task(Unit)
    }

    fun signOut() {
        currentUser = null
    }

    companion object {
        private val instance = FirebaseAuth()
        fun getInstance(): FirebaseAuth = instance
    }
}

class Task<T>(private val result: T? = null) {
    fun addOnSuccessListener(listener: (T) -> Unit): Task<T> {
        result?.let { listener(it) }
        return this
    }
    fun addOnFailureListener(listener: (Exception) -> Unit): Task<T> {
        return this
    }
    fun addOnCompleteListener(listener: (Task<T>) -> Unit): Task<T> {
        listener(this)
        return this
    }
    fun isSuccessful(): Boolean = true
    fun getResult(): T? = result
    fun getException(): Exception? = null
}
