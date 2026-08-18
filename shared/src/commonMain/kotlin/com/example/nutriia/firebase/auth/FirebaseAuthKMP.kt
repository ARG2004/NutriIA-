package com.example.nutriia.firebase.auth

import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class FirebaseAuth private constructor() {
    companion object {
        private var _instance: FirebaseAuth? = null
        fun getInstance(): FirebaseAuth = _instance ?: FirebaseAuth().also { _instance = it }
    }

    private val delegate get() = Firebase.auth

    val currentUser: FirebaseUser?
        get() = delegate.currentUser?.let { FirebaseUser(it) }

    val authStateChanged: Flow<FirebaseUser?>
        get() = delegate.authStateChanged.map { user -> user?.let { FirebaseUser(it) } }

    suspend fun signInWithEmailAndPassword(email: String, pass: String): AuthResult {
        val result = delegate.signInWithEmailAndPassword(email, pass)
        return AuthResult(result.user?.let { FirebaseUser(it) })
    }

    suspend fun createUserWithEmailAndPassword(email: String, pass: String): AuthResult {
        val result = delegate.createUserWithEmailAndPassword(email, pass)
        return AuthResult(result.user?.let { FirebaseUser(it) })
    }

    suspend fun sendPasswordResetEmail(email: String) {
        delegate.sendPasswordResetEmail(email)
    }

    suspend fun signOut() {
        delegate.signOut()
    }
}

class FirebaseUser(private val delegate: dev.gitlive.firebase.auth.FirebaseUser) {
    val uid: String get() = delegate.uid
    val email: String? get() = delegate.email
    val displayName: String? get() = delegate.displayName

    suspend fun reload() {
        delegate.reload()
    }

    suspend fun updatePassword(pass: String) {
        delegate.updatePassword(pass)
    }

    suspend fun reauthenticate(cred: Any?): Any = Unit

    suspend fun delete() {
        delegate.delete()
    }
}

class AuthResult(val user: FirebaseUser?)

object EmailAuthProvider {
    fun credential(email: String, pass: String): Any = Unit
    fun getCredential(email: String, pass: String): Any = Unit
}
