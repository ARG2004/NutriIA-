package com.example.nutriia.ginecologo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.embarazo.RegistroPesoEmbarazo
import com.example.nutriia.embarazo.RegistroSintomasEmbarazo
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

data class ExpedienteEmbarazoUiState(
    val cargando: Boolean = true,
    val perfil: PerfilEmbarazo? = null,
    val registrosPeso: List<RegistroPesoEmbarazo> = emptyList(),
    val registrosSintomas: List<RegistroSintomasEmbarazo> = emptyList(),
    val citas: List<CitaEmbarazo> = emptyList(),
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class PacienteExpedienteEmbarazoViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    private val _mamaUid = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ExpedienteEmbarazoUiState> = _mamaUid.flatMapLatest { uid ->
        if (uid == null) {
            flowOf(ExpedienteEmbarazoUiState(cargando = false))
        } else {
            val perfilFlow = callbackFlow {
                val listener = db.collection("usuarios")
                    .document(uid)
                    .collection("perfilEmbarazo")
                    .document("unico")
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            trySend(null)
                            return@addSnapshotListener
                        }
                        val p = snap?.data?.let { PerfilEmbarazo.fromMap(it) }
                        trySend(p)
                    }
                awaitClose { listener.remove() }
            }

            val pesoFlow = callbackFlow {
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
            }

            val sintomasFlow = callbackFlow {
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
            }

            val citasFlow = callbackFlow {
                val listener = db.collection("usuarios")
                    .document(uid)
                    .collection("perfilEmbarazo")
                    .document("unico")
                    .collection("citas")
                    .addSnapshotListener { snap, err ->
                        if (err != null) {
                            trySend(emptyList())
                            return@addSnapshotListener
                        }
                        val list = snap?.documents?.mapNotNull { doc ->
                            doc.data?.let { CitaEmbarazo.fromMap(doc.id, it) }
                        } ?: emptyList()
                        trySend(list)
                    }
                awaitClose { listener.remove() }
            }

            combine(perfilFlow, pesoFlow, sintomasFlow, citasFlow) { perfil, peso, sintomas, citas ->
                ExpedienteEmbarazoUiState(
                    cargando = false,
                    perfil = perfil,
                    registrosPeso = peso,
                    registrosSintomas = sintomas,
                    citas = citas
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ExpedienteEmbarazoUiState(cargando = true))

    fun setMamaUid(uid: String) {
        _mamaUid.value = uid
    }
}
