package com.example.nutriia.auth

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

/**
 * Verificador de Cédulas Profesionales mediante WebView embebido que carga
 * la página oficial del Registro Nacional de Profesionistas (SEP).
 *
 * Cumplimiento Legal (LFPDPPP):
 * - Utiliza el portal público oficial de la SEP tal como lo haría un usuario en su navegador.
 * - No desactiva validación SSL/TLS ni suplanta identidad.
 * - No realiza scraping de sitios privados ni de terceros.
 * - La consulta se ejecuta dentro de un WebView estándar de Android (motor Chromium).
 * - Se obtienen únicamente los datos públicos que el portal muestra a cualquier ciudadano.
 */

data class ResultadoCedula(
    val valida: Boolean,
    val cedula: String = "",
    val nombreTitular: String = "",
    val genero: String = "",
    val institucion: String = "",
    val profesion: String = "",
    val entidad: String = "",
    val anoRegistro: String = "",
    val mensaje: String = ""
)

object CedulaVerifier {
    private const val TAG = "CedulaVerifier"
    private const val SEP_URL = "https://cedulaprofesional.sep.gob.mx/cedula/presidencia/indexAvanzada.action"

    private var ultimoIntentoMs: Long = 0L
    private const val RATE_LIMIT_MS: Long = 3000L

    suspend fun verificarCedulaConRateLimit(cedula: String): ResultadoCedula {
        val ahora = System.currentTimeMillis()
        val diferencia = ahora - ultimoIntentoMs
        if (diferencia in 1..<RATE_LIMIT_MS) {
            kotlinx.coroutines.delay(RATE_LIMIT_MS - diferencia)
        }
        ultimoIntentoMs = System.currentTimeMillis()
        return verificarCedula(cedula)
    }

    suspend fun verificarCedula(cedula: String): ResultadoCedula {
        val cedulaLimpia = cedula.filter(Char::isDigit).trim()
        if (cedulaLimpia.length < 6) {
            return ResultadoCedula(
                valida = false,
                mensaje = "La cédula debe contener al menos 6 dígitos numéricos"
            )
        }

        return try {
            verificarConWebView(cedulaLimpia)
        } catch (e: Exception) {
            Log.e(TAG, "Error general en verificación: ${e.message}")
            ResultadoCedula(
                valida = false,
                mensaje = "Error al verificar la cédula. Intenta de nuevo."
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun verificarConWebView(cedula: String): ResultadoCedula {
        val deferred = CompletableDeferred<ResultadoCedula>()

        withContext(Dispatchers.Main) {
            val context = appContext ?: run {
                deferred.complete(ResultadoCedula(valida = false, mensaje = "Contexto no disponible"))
                return@withContext
            }

            val webView = WebView(context)
            val settings = webView.settings
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.cacheMode = WebSettings.LOAD_NO_CACHE
            settings.userAgentString = "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0 Mobile Safari/537.36"

            val jsInterface = CedulaJsInterface(cedula, deferred, webView)
            webView.addJavascriptInterface(jsInterface, "NutriIA")

            webView.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    Log.d(TAG, "[WEBVIEW] Página cargada: $url")
                    // Esperar a que Angular renderice y luego buscar
                    Handler(Looper.getMainLooper()).postDelayed({
                        buscarCedulaEnAngular(webView, cedula)
                    }, 2000)
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    return false // Permitir navegación normal
                }

                override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
                    Log.e(TAG, "[WEBVIEW] Error: $description")
                    if (!deferred.isCompleted) {
                        deferred.complete(ResultadoCedula(valida = false, mensaje = "Error al cargar el portal de la SEP."))
                    }
                    view?.destroy()
                }
            }

            Log.d(TAG, "[WEBVIEW] Cargando portal SEP para cédula: $cedula")
            webView.loadUrl(SEP_URL)

            // Timeout de seguridad: 15 segundos máximo
            Handler(Looper.getMainLooper()).postDelayed({
                if (!deferred.isCompleted) {
                    Log.w(TAG, "[WEBVIEW] Timeout alcanzado")
                    deferred.complete(ResultadoCedula(valida = false, mensaje = "Tiempo de espera agotado. Intenta de nuevo."))
                    webView.destroy()
                }
            }, 25000)
        }

        return withTimeoutOrNull(30000L) { deferred.await() }
            ?: ResultadoCedula(valida = false, mensaje = "Tiempo de espera agotado al verificar cédula.")
    }

    private fun buscarCedulaEnAngular(webView: WebView, cedula: String) {
        val js = """
            (function() {
                try {
                    // 1. Click en pestaña "Número de cédula"
                    var tabs = document.querySelectorAll('.mat-tab-label, [role="tab"], a[class*="tab"], .nav-link, li a, ul.nav li a');
                    NutriIA.onLog('[WEBVIEW-JS] Tabs encontrados: ' + tabs.length);
                    for (var i = 0; i < tabs.length; i++) {
                        var txt = tabs[i].textContent.trim();
                        NutriIA.onLog('[WEBVIEW-JS] Tab ' + i + ': ' + txt);
                        if (txt.indexOf('mero de c') >= 0 || txt.indexOf('mero de C') >= 0 ||
                            txt.indexOf('edula') >= 0) {
                            tabs[i].click();
                            NutriIA.onLog('[WEBVIEW-JS] Tab clickeado: ' + txt);
                            break;
                        }
                    }
                    
                    // 2. Esperar y llenar el campo de cédula
                    setTimeout(function() {
                        var inputs = document.querySelectorAll('input');
                        NutriIA.onLog('[WEBVIEW-JS] Inputs encontrados: ' + inputs.length);
                        var cedulaInput = null;
                        
                        for (var j = 0; j < inputs.length; j++) {
                            var ph = (inputs[j].placeholder || '').toLowerCase();
                            var nm = (inputs[j].name || '').toLowerCase();
                            var id = (inputs[j].id || '').toLowerCase();
                            var tp = inputs[j].type || '';
                            var visible = inputs[j].offsetParent !== null;
                            NutriIA.onLog('[WEBVIEW-JS] Input ' + j + ': type=' + tp + ' name=' + nm + ' id=' + id + ' ph=' + ph + ' visible=' + visible);
                            
                            if (visible && tp !== 'hidden' && (
                                ph.indexOf('dula') >= 0 || nm.indexOf('cedula') >= 0 || nm.indexOf('idcedula') >= 0 ||
                                id.indexOf('cedula') >= 0 || id.indexOf('idcedula') >= 0 || tp === 'number' || tp === 'text')) {
                                cedulaInput = inputs[j];
                                break;
                            }
                        }
                        
                        if (!cedulaInput) {
                            for (var k = 0; k < inputs.length; k++) {
                                if (inputs[k].offsetParent !== null && inputs[k].type !== 'hidden') {
                                    cedulaInput = inputs[k];
                                    break;
                                }
                            }
                        }
                        
                        if (cedulaInput) {
                            // Llenar valor con setter nativo (compatible con Angular reactive forms)
                            var nativeSet = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
                            nativeSet.call(cedulaInput, '$cedula');
                            cedulaInput.dispatchEvent(new Event('input', { bubbles: true }));
                            cedulaInput.dispatchEvent(new Event('change', { bubbles: true }));
                            // También KeyboardEvent por si Angular lo necesita
                            cedulaInput.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true }));
                            
                            NutriIA.onLog('[WEBVIEW-JS] Campo llenado con: $cedula');
                            
                            // 3. Click en Buscar
                            setTimeout(function() {
                                var botones = document.querySelectorAll('button, input[type="submit"]');
                                var clicked = false;
                                for (var b = 0; b < botones.length; b++) {
                                    var txt = botones[b].textContent.trim().toLowerCase();
                                    NutriIA.onLog('[WEBVIEW-JS] Botón ' + b + ': ' + txt);
                                    if (txt.indexOf('buscar') >= 0 || txt.indexOf('consultar') >= 0) {
                                        botones[b].click();
                                        clicked = true;
                                        NutriIA.onLog('[WEBVIEW-JS] Botón Buscar clickeado');
                                        break;
                                    }
                                }
                                if (!clicked) {
                                    // Fallback: submit del form
                                    var forms = document.querySelectorAll('form');
                                    if (forms.length > 0) {
                                        forms[0].submit();
                                        NutriIA.onLog('[WEBVIEW-JS] Form submit ejecutado');
                                    }
                                }
                                
                                // 4. Polling: intentar extraer resultados hasta 4 veces con 2s de intervalo
                                var intentos = 0;
                                var maxIntentos = 4;
                                
                                function intentarExtraer() {
                                    intentos++;
                                    NutriIA.onLog('[WEBVIEW-JS] Intento de extracción #' + intentos);
                                    
                                    // Loguear un snippet del HTML actual del body
                                    var bodyHtml = document.body.innerHTML || '';
                                    NutriIA.onLog('[WEBVIEW-JS] Body HTML len: ' + bodyHtml.length);
                                    
                                    // Buscar tabla de resultados
                                    var tables = document.querySelectorAll('table');
                                    NutriIA.onLog('[WEBVIEW-JS] Tablas encontradas: ' + tables.length);
                                    
                                    var found = false;
                                    
                                    for (var t = 0; t < tables.length; t++) {
                                        var rows = tables[t].querySelectorAll('tr');
                                        NutriIA.onLog('[WEBVIEW-JS] Tabla ' + t + ': ' + rows.length + ' filas');
                                        
                                        for (var r = 0; r < rows.length; r++) {
                                            var cells = rows[r].querySelectorAll('td');
                                            if (cells.length >= 3) {
                                                var textos = [];
                                                for (var c = 0; c < cells.length; c++) {
                                                    textos.push(cells[c].textContent.trim());
                                                }
                                                var filaTexto = textos.join('|');
                                                NutriIA.onLog('[WEBVIEW-JS] Fila datos: ' + filaTexto);
                                                
                                                if (filaTexto.indexOf('$cedula') >= 0) {
                                                    // ¡Encontrada!
                                                    var resultado = {
                                                        valida: true,
                                                        cedula: textos[0] || '$cedula',
                                                        nombreTitular: [textos[1] || '', textos[2] || '', textos[3] || ''].filter(function(s) { return s.length > 0; }).join(' '),
                                                        genero: textos[4] || '',
                                                        institucion: textos[5] || '',
                                                        profesion: textos[6] || '',
                                                        entidad: textos[7] || '',
                                                        anoRegistro: textos[8] || '',
                                                        mensaje: 'Cédula oficial verificada en el Registro Nacional SEP'
                                                    };
                                                    NutriIA.onLog('[WEBVIEW-JS] ÉXITO: ' + JSON.stringify(resultado));
                                                    NutriIA.onResultado(JSON.stringify(resultado));
                                                    found = true;
                                                    return;
                                                }
                                            }
                                        }
                                    }
                                    
                                    // También buscar en texto plano de la página
                                    if (!found) {
                                        var pageText = document.body.innerText || '';
                                        if (pageText.indexOf('$cedula') >= 0 && 
                                            (pageText.indexOf('LICENCIATURA') >= 0 || pageText.indexOf('UNIVERSIDAD') >= 0 || 
                                             pageText.indexOf('MAESTR') >= 0 || pageText.indexOf('Resultados') >= 0)) {
                                            NutriIA.onLog('[WEBVIEW-JS] Datos en texto plano, enviando...');
                                            NutriIA.onPageText(pageText);
                                            return;
                                        }
                                    }
                                    
                                    if (!found && intentos < maxIntentos) {
                                        NutriIA.onLog('[WEBVIEW-JS] Reintentando en 2s...');
                                        setTimeout(intentarExtraer, 2000);
                                    } else if (!found) {
                                        // Último recurso: enviar todo el texto de la página
                                        var fullText = document.body.innerText || '';
                                        NutriIA.onLog('[WEBVIEW-JS] Texto completo (500 chars): ' + fullText.substring(0, 500));
                                        NutriIA.onResultado('{"valida":false,"mensaje":"Cédula no encontrada en el Registro Nacional de Profesiones de la SEP"}');
                                    }
                                }
                                
                                // Primera extracción después de 3s
                                setTimeout(intentarExtraer, 3000);
                            }, 500);
                        } else {
                            NutriIA.onLog('[WEBVIEW-JS] No se encontró campo de cédula');
                            NutriIA.onResultado('{"valida":false,"mensaje":"No se pudo encontrar el campo de búsqueda en el portal SEP."}');
                        }
                    }, 1500);
                } catch(e) {
                    NutriIA.onLog('[WEBVIEW-JS] Error: ' + e.message);
                    NutriIA.onResultado('{"valida":false,"mensaje":"Error al interactuar con el portal SEP."}');
                }
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }

    // ── Context holder ──────────────────────────────────────────────────────────
    private var appContext: Context? = null
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // ── JavaScript Interface ────────────────────────────────────────────────────
    private class CedulaJsInterface(
        private val cedula: String,
        private val deferred: CompletableDeferred<ResultadoCedula>,
        private val webView: WebView
    ) {
        @JavascriptInterface
        fun onLog(message: String) {
            Log.d(TAG, message)
        }

        @JavascriptInterface
        fun onResultado(jsonStr: String) {
            try {
                val json = JSONObject(jsonStr)
                val resultado = ResultadoCedula(
                    valida = json.optBoolean("valida", false),
                    cedula = json.optString("cedula", cedula),
                    nombreTitular = json.optString("nombreTitular", ""),
                    genero = json.optString("genero", ""),
                    institucion = json.optString("institucion", ""),
                    profesion = json.optString("profesion", ""),
                    entidad = json.optString("entidad", ""),
                    anoRegistro = json.optString("anoRegistro", ""),
                    mensaje = json.optString("mensaje", "")
                )
                Log.d(TAG, "[WEBVIEW] Resultado: valida=${resultado.valida}, nombre=${resultado.nombreTitular}, profesion=${resultado.profesion}")
                if (!deferred.isCompleted) deferred.complete(resultado)
            } catch (e: Exception) {
                Log.e(TAG, "[WEBVIEW] Error parseando resultado: ${e.message}")
                if (!deferred.isCompleted) {
                    deferred.complete(ResultadoCedula(valida = false, mensaje = "Error procesando resultado."))
                }
            }
            Handler(Looper.getMainLooper()).post { webView.destroy() }
        }

        @JavascriptInterface
        fun onPageText(pageText: String) {
            // Intentar extraer datos del texto plano de la página
            try {
                val lines = pageText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                val cedulaIdx = lines.indexOfFirst { it.contains(cedula) }

                if (cedulaIdx >= 0) {
                    // Buscar datos cercanos a la línea de la cédula
                    val nearby = lines.subList(
                        maxOf(0, cedulaIdx - 5),
                        minOf(lines.size, cedulaIdx + 15)
                    )
                    Log.d(TAG, "[WEBVIEW] Texto cercano: ${nearby.joinToString(" | ")}")

                    val nombre = nearby.filter { 
                        !it.contains(cedula) && 
                        !it.equals("FEMENINO", true) && !it.equals("MASCULINO", true) &&
                        !it.contains("UNIVERSIDAD", true) && !it.contains("LICENCIATURA", true) &&
                        !it.contains("MAESTRÍA", true) && !it.matches(Regex("^\\d{4}$")) &&
                        !it.contains("Buscar", true) && !it.contains("Resultados", true) &&
                        it.length in 2..50 && it.all { c -> c.isLetter() || c.isWhitespace() }
                    }.take(3).joinToString(" ")

                    val genero = nearby.firstOrNull { it.equals("FEMENINO", true) || it.equals("MASCULINO", true) } ?: ""
                    val institucion = nearby.firstOrNull { it.contains("UNIVERSIDAD", true) || it.contains("INSTITUTO", true) || it.contains("ESCUELA", true) } ?: ""
                    val profesion = nearby.firstOrNull { it.contains("LICENCIATURA", true) || it.contains("MAESTRÍA", true) || it.contains("DOCTORADO", true) || it.contains("INGENIERÍA", true) } ?: ""
                    val anio = nearby.firstOrNull { it.matches(Regex("^(19|20)\\d{2}$")) } ?: ""

                    val resultado = ResultadoCedula(
                        valida = nombre.isNotBlank() || profesion.isNotBlank(),
                        cedula = cedula,
                        nombreTitular = nombre,
                        genero = genero,
                        institucion = institucion,
                        profesion = profesion,
                        anoRegistro = anio,
                        mensaje = if (nombre.isNotBlank()) "Cédula oficial verificada en el Registro Nacional SEP" else "Cédula no encontrada"
                    )
                    if (!deferred.isCompleted) deferred.complete(resultado)
                } else {
                    if (!deferred.isCompleted) {
                        deferred.complete(ResultadoCedula(valida = false, mensaje = "Cédula no encontrada en el Registro Nacional de Profesiones de la SEP"))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "[WEBVIEW] Error procesando texto: ${e.message}")
                if (!deferred.isCompleted) {
                    deferred.complete(ResultadoCedula(valida = false, mensaje = "Error procesando resultado."))
                }
            }
            Handler(Looper.getMainLooper()).post { webView.destroy() }
        }
    }
}
