@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.auth

import com.example.nutriia.platform.Log
import kotlinx.cinterop.cValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
import platform.UIKit.UIApplication
import platform.WebKit.WKNavigation
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

private const val TAG = "CedulaVerifier_iOS"
private const val SEP_URL = "https://cedulaprofesional.sep.gob.mx/cedula/presidencia/indexAvanzada.action"

actual suspend fun verificarEnPortalSEP(cedula: String): ResultadoCedula {
    val deferred = CompletableDeferred<ResultadoCedula>()

    withContext(Dispatchers.Main) {
        try {
            val config = WKWebViewConfiguration()
            val userController = WKUserContentController()

            var webViewRef: WKWebView? = null

            fun cleanup() {
                try {
                    webViewRef?.removeFromSuperview()
                    webViewRef?.stopLoading()
                    userController.removeScriptMessageHandlerForName("NutriIA")
                    webViewRef = null
                } catch (_: Throwable) {}
            }

            val handler = object : NSObject(), WKScriptMessageHandlerProtocol {
                override fun userContentController(
                    userContentController: WKUserContentController,
                    didReceiveScriptMessage: WKScriptMessage
                ) {
                    val bodyStr = didReceiveScriptMessage.body?.toString() ?: ""
                    try {
                        if (bodyStr.contains("\"type\":\"log\"")) {
                            Log.d(TAG, "[WEBVIEW-JS] $bodyStr")
                            return
                        }

                        if (bodyStr.contains("\"type\":\"pageText\"")) {
                            val pageText = extractField(bodyStr, "data")
                            processPageText(pageText, cedula, deferred)
                            cleanup()
                            return
                        }

                        // Mensaje de resultado directo
                        val valida = bodyStr.contains("\"valida\":true")
                        val resultado = ResultadoCedula(
                            valida = valida,
                            cedula = extractField(bodyStr, "cedula").ifBlank { cedula },
                            nombreTitular = extractField(bodyStr, "nombreTitular"),
                            genero = extractField(bodyStr, "genero"),
                            institucion = extractField(bodyStr, "institucion"),
                            profesion = extractField(bodyStr, "profesion"),
                            entidad = extractField(bodyStr, "entidad"),
                            anoRegistro = extractField(bodyStr, "anoRegistro"),
                            mensaje = extractField(bodyStr, "mensaje").ifBlank {
                                if (valida) "Cédula oficial verificada en el Registro Nacional SEP"
                                else "Cédula no encontrada en el Registro Nacional de Profesiones de la SEP"
                            }
                        )
                        Log.d(TAG, "[WEBVIEW] Resultado recibido: valida=${resultado.valida}, titular=${resultado.nombreTitular}")
                        if (!deferred.isCompleted) deferred.complete(resultado)
                        cleanup()
                    } catch (e: Throwable) {
                        Log.e(TAG, "[WEBVIEW] Error al procesar mensaje: ${e.message}")
                        if (!deferred.isCompleted) {
                            deferred.complete(ResultadoCedula(valida = false, mensaje = "Error al procesar la respuesta de la SEP"))
                        }
                        cleanup()
                    }
                }
            }

            userController.addScriptMessageHandler(handler, "NutriIA")
            config.userContentController = userController

            val webView = WKWebView(
                frame = cValue { CGRectMake(0.0, 0.0, 50.0, 50.0) },
                configuration = config
            )
            webView.alpha = 0.01
            webView.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
            webViewRef = webView

            // Agregar a la vista activa para garantizar ciclo de vida de renderizado WebKit
            val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
                ?: (UIApplication.sharedApplication.windows.firstOrNull() as? platform.UIKit.UIWindow)?.rootViewController
            rootVC?.view?.addSubview(webView)

            val navDelegate = object : NSObject(), WKNavigationDelegateProtocol {
                override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                    Log.d(TAG, "[WEBVIEW] Página SEP cargada con éxito. Ejecutando script de búsqueda Angular...")

                    val js = """
                        (function() {
                            try {
                                function postLog(msg) {
                                    window.webkit.messageHandlers.NutriIA.postMessage(JSON.stringify({ type: 'log', message: msg }));
                                }
                                function postResultado(resObj) {
                                    window.webkit.messageHandlers.NutriIA.postMessage(JSON.stringify(resObj));
                                }
                                function postPageText(txt) {
                                    window.webkit.messageHandlers.NutriIA.postMessage(JSON.stringify({ type: 'pageText', data: txt }));
                                }

                                postLog('Iniciando búsqueda de cédula: $cedula');

                                // 1. Click en pestaña "Número de cédula"
                                var tabs = document.querySelectorAll('.mat-tab-label, [role="tab"], a[class*="tab"], .nav-link, li a, ul.nav li a');
                                postLog('Tabs encontrados: ' + tabs.length);
                                for (var i = 0; i < tabs.length; i++) {
                                    var txt = tabs[i].textContent.trim();
                                    if (txt.indexOf('mero de c') >= 0 || txt.indexOf('mero de C') >= 0 || txt.indexOf('edula') >= 0) {
                                        tabs[i].click();
                                        postLog('Tab clickeado: ' + txt);
                                        break;
                                    }
                                }

                                // 2. Esperar a que Angular monte el input
                                setTimeout(function() {
                                    var inputs = document.querySelectorAll('input');
                                    postLog('Inputs encontrados: ' + inputs.length);
                                    var cedulaInput = null;
                                    for (var j = 0; j < inputs.length; j++) {
                                        var ph = (inputs[j].placeholder || '').toLowerCase();
                                        var nm = (inputs[j].name || '').toLowerCase();
                                        var id = (inputs[j].id || '').toLowerCase();
                                        var tp = inputs[j].type || '';
                                        var visible = inputs[j].offsetParent !== null;
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
                                        var nativeSet = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value').set;
                                        nativeSet.call(cedulaInput, '$cedula');
                                        cedulaInput.dispatchEvent(new Event('input', { bubbles: true }));
                                        cedulaInput.dispatchEvent(new Event('change', { bubbles: true }));
                                        cedulaInput.dispatchEvent(new KeyboardEvent('keyup', { bubbles: true }));
                                        postLog('Campo llenado con cédula: $cedula');

                                        // 3. Click en Buscar
                                        setTimeout(function() {
                                            var botones = document.querySelectorAll('button, input[type="submit"]');
                                            var clicked = false;
                                            for (var b = 0; b < botones.length; b++) {
                                                var txt = botones[b].textContent.trim().toLowerCase();
                                                if (txt.indexOf('buscar') >= 0 || txt.indexOf('consultar') >= 0) {
                                                    botones[b].click();
                                                    clicked = true;
                                                    postLog('Botón Buscar clickeado');
                                                    break;
                                                }
                                            }
                                            if (!clicked) {
                                                var forms = document.querySelectorAll('form');
                                                if (forms.length > 0) {
                                                    forms[0].submit();
                                                    postLog('Form submit alternativo ejecutado');
                                                }
                                            }

                                            // 4. Polling con reintentos para extraer resultados
                                            var intentos = 0;
                                            var maxIntentos = 4;
                                            function intentarExtraer() {
                                                intentos++;
                                                postLog('Intento de extracción #' + intentos);
                                                var tables = document.querySelectorAll('table');
                                                var found = false;

                                                for (var t = 0; t < tables.length; t++) {
                                                    var rows = tables[t].querySelectorAll('tr');
                                                    for (var r = 0; r < rows.length; r++) {
                                                        var cells = rows[r].querySelectorAll('td');
                                                        if (cells.length >= 3) {
                                                            var textos = [];
                                                            for (var c = 0; c < cells.length; c++) {
                                                                textos.push(cells[c].textContent.trim());
                                                            }
                                                            var filaTexto = textos.join('|');
                                                            if (filaTexto.indexOf('$cedula') >= 0) {
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
                                                                postResultado(resultado);
                                                                found = true;
                                                                return;
                                                            }
                                                        }
                                                    }
                                                }

                                                if (!found) {
                                                    var pageText = document.body.innerText || '';
                                                    if (pageText.indexOf('$cedula') >= 0 && 
                                                        (pageText.indexOf('LICENCIATURA') >= 0 || pageText.indexOf('UNIVERSIDAD') >= 0 || 
                                                         pageText.indexOf('MAESTR') >= 0 || pageText.indexOf('Resultados') >= 0)) {
                                                        postPageText(pageText);
                                                        return;
                                                    }
                                                }

                                                if (!found && intentos < maxIntentos) {
                                                    setTimeout(intentarExtraer, 2000);
                                                } else if (!found) {
                                                    postResultado({
                                                        valida: false,
                                                        mensaje: 'Cédula no encontrada en el Registro Nacional de Profesiones de la SEP'
                                                    });
                                                }
                                            }

                                            setTimeout(intentarExtraer, 2500);
                                        }, 600);
                                    } else {
                                        postResultado({
                                            valida: false,
                                            mensaje: 'No se pudo encontrar el campo de búsqueda en el portal SEP.'
                                        });
                                    }
                                }, 1500);
                            } catch (e) {
                                postResultado({
                                    valida: false,
                                    mensaje: 'Error interactuando con el portal SEP: ' + e.message
                                });
                            }
                        })();
                    """.trimIndent()

                    webView.evaluateJavaScript(js, null)
                }

                override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
                    Log.e(TAG, "[WEBVIEW] Error de navegación: ${withError.localizedDescription}")
                    if (!deferred.isCompleted) {
                        deferred.complete(ResultadoCedula(valida = false, mensaje = "Error de red al conectar con el portal de la SEP"))
                    }
                    cleanup()
                }
            }

            webView.navigationDelegate = navDelegate

            val url = NSURL.URLWithString(SEP_URL)
            if (url != null) {
                Log.d(TAG, "[WEBVIEW] Iniciando carga de URL: $SEP_URL")
                webView.loadRequest(NSURLRequest.requestWithURL(url))
            } else {
                deferred.complete(ResultadoCedula(valida = false, mensaje = "URL inválida"))
                cleanup()
            }
        } catch (e: Throwable) {
            Log.e(TAG, "[WEBVIEW] Excepción inicializando WKWebView: ${e.message}")
            if (!deferred.isCompleted) {
                deferred.complete(ResultadoCedula(valida = false, mensaje = "Excepción al inicializar verificación: ${e.message}"))
            }
        }
    }

    return withTimeoutOrNull(30000L) { deferred.await() }
        ?: ResultadoCedula(valida = false, mensaje = "Tiempo de espera agotado al verificar cédula.")
}

private fun extractField(jsonStr: String, key: String): String {
    val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
    return pattern.find(jsonStr)?.groupValues?.get(1) ?: ""
}

private fun processPageText(pageText: String, cedula: String, deferred: CompletableDeferred<ResultadoCedula>) {
    try {
        val lines = pageText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
        val cedulaIdx = lines.indexOfFirst { it.contains(cedula) }

        if (cedulaIdx >= 0) {
            val nearby = lines.subList(
                maxOf(0, cedulaIdx - 5),
                minOf(lines.size, cedulaIdx + 15)
            )
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
    } catch (e: Throwable) {
        if (!deferred.isCompleted) {
            deferred.complete(ResultadoCedula(valida = false, mensaje = "Error procesando texto de la SEP"))
        }
    }
}
