package androidx.lifecycle.viewmodel.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel

@Composable
inline fun <reified VM : ViewModel> viewModel(
    key: String? = null,
    noinline factory: Any? = null
): VM {
    return remember(key) {
        val app = android.app.Application()
        when (VM::class.simpleName) {
            "AccessibilityViewModel" -> com.example.nutriia.accesibilidad.AccessibilityViewModel(app) as VM
            "AlertaViewModel" -> com.example.nutriia.alerta.AlertaViewModel(app) as VM
            "AnalisisViewModel" -> com.example.nutriia.analisisIA.AnalisisViewModel() as VM
            "LoginViewModel" -> com.example.nutriia.auth.LoginViewModel(app) as VM
            "RegisterViewModel" -> com.example.nutriia.auth.RegisterViewModel(app) as VM
            "ChatViewModel" -> com.example.nutriia.chatbot.ChatViewModel() as VM
            "CrecimientoViewModel" -> com.example.nutriia.crecimiento.CrecimientoViewModel(app) as VM
            "NutritionistDashboardViewModel" -> com.example.nutriia.dashboard.NutritionistDashboardViewModel() as VM
            "GinecologoViewModel" -> com.example.nutriia.ginecologo.GinecologoViewModel() as VM
            "GinecologoDashboardViewModel" -> com.example.nutriia.ginecologo.GinecologoDashboardViewModel() as VM
            "PacienteExpedienteEmbarazoViewModel" -> com.example.nutriia.ginecologo.PacienteExpedienteEmbarazoViewModel() as VM
            "EmbarazoDashboardViewModel" -> com.example.nutriia.embarazo.EmbarazoDashboardViewModel() as VM
            "PacienteExpedienteViewModel" -> com.example.nutriia.expediente.PacienteExpedienteViewModel() as VM
            "LactanciaViewModel" -> com.example.nutriia.lactancia.LactanciaViewModel(app) as VM
            "NutrientesViewModel" -> com.example.nutriia.nutriente.NutrientesViewModel(app) as VM
            "PaymentViewModel" -> com.example.nutriia.payment.PaymentViewModel() as VM
            "PediatraDashboardViewModel" -> com.example.nutriia.pediatra.PediatraDashboardViewModel() as VM
            "NutriSharedViewModel" -> com.example.nutriia.shared.NutriSharedViewModel(app) as VM
            "AlimentacionViewModel" -> com.example.nutriia.solidos.AlimentacionViewModel(app) as VM
            "TeleconsultaViewModel" -> com.example.nutriia.teleconsulta.TeleconsultaViewModel(app) as VM
            "VinculacionViewModel" -> com.example.nutriia.vinculacion.VinculacionViewModel() as VM
            else -> object : ViewModel() {} as VM
        }
    }
}
