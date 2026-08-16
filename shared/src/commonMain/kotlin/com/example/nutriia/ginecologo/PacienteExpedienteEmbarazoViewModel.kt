package com.example.nutriia.ginecologo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nutriia.embarazo.PerfilEmbarazo
import com.example.nutriia.embarazo.RegistroPesoEmbarazo
import com.example.nutriia.embarazo.RegistroSintomasEmbarazo
import com.example.nutriia.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
            val perfilFlow = flow {
                val snap = db.collection("usuarios").document(uid).collection("perfilEmbarazo").document("unico").get()
                emit(PerfilEmbarazo.fromMap(snap.data))
            }

            val pesoFlow = flow {
                val snap = db.collection("usuarios").document(uid).collection("perfilEmbarazo").document("unico").collection("registrosPeso").get()
                val list = snap.documents.mapNotNull { doc -> RegistroPesoEmbarazo.fromMap(doc.id, doc.data) }
                emit(list)
            }

            val sintomasFlow = flow {
                val snap = db.collection("usuarios").document(uid).collection("perfilEmbarazo").document("unico").collection("registrosSintomas").get()
                val list = snap.documents.mapNotNull { doc -> RegistroSintomasEmbarazo.fromMap(doc.id, doc.data) }
                emit(list)
            }

            val citasFlow = flow {
                val snap = db.collection("usuarios").document(uid).collection("perfilEmbarazo").document("unico").collection("citas").get()
                val list = snap.documents.mapNotNull { doc -> CitaEmbarazo.fromMap(doc.id, doc.data) }
                emit(list)
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
