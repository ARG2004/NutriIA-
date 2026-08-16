@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.example.nutriia.auth

import kotlinx.cinterop.cValue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSURLRequest
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

actual object PlatformCedulaVerifier {
    private const val SEP_URL = "https://cedulaprofesional.sep.gob.mx/cedula/presidencia/indexAvanzada.action"

    actual suspend fun verificarCedulaNativa(cedula: String): ResultadoCedula {
        val deferred = CompletableDeferred<ResultadoCedula>()

        withContext(Dispatchers.Main) {
            try {
                val config = WKWebViewConfiguration()
                val userController = WKUserContentController()

                val handler = object : NSObject(), WKScriptMessageHandlerProtocol {
                    override fun userContentController(
                        userContentController: WKUserContentController,
                        didReceiveScriptMessage: WKScriptMessage
                    ) {
                        val bodyStr = didReceiveScriptMessage.body?.toString() ?: ""
                        parseAndComplete(bodyStr, cedula, deferred)
                    }
                }

                userController.addScriptMessageHandler(handler, "NutriIA")
                config.userContentController = userController

                val webView = WKWebView(frame = cValue { CGRectZero }, configuration = config)
                webView.customUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.0 Mobile/15E148 Safari/604.1"

                val navDelegate = object : NSObject(), WKNavigationDelegateProtocol {
                    override fun webView(webView: WKWebView, didFinishNavigation: WKNavigation?) {
                        val js = """
                            (function() {
                                try {
                                    var tabs = document.querySelectorAll('.mat-tab-label, [role="tab"], a[class*="tab"], .nav-link, li a, ul.nav li a');
                                    for (var i = 0; i < tabs.length; i++) {
                                        var txt = tabs[i].textContent.trim();
                                        if (txt.indexOf('mero de c') >= 0 || txt.indexOf('mero de C') >= 0 || txt.indexOf('edula') >= 0) {
                                            tabs[i].click();
                                            break;
                                        }
                                    }
                                    setTimeout(function() {
                                        var inputs = document.querySelectorAll('input');
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
                                            
                                            setTimeout(function() {
                                                var botones = document.querySelectorAll('button, input[type="submit"]');
                                                var clicked = false;
                                                for (var b = 0; b < botones.length; b++) {
                                                    var txt = botones[b].textContent.trim().toLowerCase();
                                                    if (txt.indexOf('buscar') >= 0 || txt.indexOf('consultar') >= 0) {
                                                        botones[b].click();
                                                        clicked = true;
                                                        break;
                                                    }
                                                }
                                                if (!clicked) {
                                                    var forms = document.querySelectorAll('form');
                                                    if (forms.length > 0) {
                                                        forms[0].submit();
                                                    }
                                                }
                                                var intentos = 0;
                                                var maxIntentos = 4;
                                                function intentarExtraer() {
                                                    intentos++;
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
                                                                    window.webkit.messageHandlers.NutriIA.postMessage(JSON.stringify(resultado));
                                                                    found = true;
                                                                    return;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    if (!found && intentos < maxIntentos) {
                                                        setTimeout(intentarExtraer, 2000);
                                                    } else if (!found) {
                                                        window.webkit.messageHandlers.NutriIA.postMessage(JSON.stringify({
                                                            valida: false,
                                                            mensaje: 'Cédula no encontrada en el Registro Nacional de Profesiones de la SEP'
                                                        }));
                                                    }
                                                }
                                                setTimeout(intentarExtraer, 2500);
                                            }, 600);
                                        } else {
                                            window.webkit.messageHandlers.NutriIA.postMessage(JSON.stringify({
                                                valida: false,
                                                mensaje: 'No se pudo encontrar el campo de búsqueda en el portal SEP.'
                                            }));
                                        }
                                    }, 1500);
                                } catch (e) {
                                    window.webkit.messageHandlers.NutriIA.postMessage(JSON.stringify({
                                        valida: false,
                                        mensaje: 'Error interactuando con el portal SEP: ' + e.message
                                    }));
                                }
                            })();
                        """.trimIndent()
                        webView.evaluateJavaScript(js, null)
                    }

                    override fun webView(webView: WKWebView, didFailNavigation: WKNavigation?, withError: NSError) {
                        if (!deferred.isCompleted) {
                            deferred.complete(ResultadoCedula(valida = false, mensaje = "Error de red al conectar con el portal de la SEP"))
                        }
                    }
                }

                webView.navigationDelegate = navDelegate

                val url = NSURL.URLWithString(SEP_URL)
                if (url != null) {
                    webView.loadRequest(NSURLRequest.requestWithURL(url))
                } else {
                    deferred.complete(ResultadoCedula(valida = false, mensaje = "URL inválida"))
                }
            } catch (e: Throwable) {
                deferred.complete(ResultadoCedula(valida = false, mensaje = "Excepción en verificación: ${e.message}"))
            }
        }

        return withTimeoutOrNull(30000L) { deferred.await() }
            ?: ResultadoCedula(valida = false, mensaje = "Tiempo de espera agotado al verificar cédula.")
    }

    private fun parseAndComplete(
        jsonStr: String,
        cedula: String,
        deferred: CompletableDeferred<ResultadoCedula>
    ) {
        try {
            val valida = jsonStr.contains("\"valida\":true")
            fun extractField(key: String): String {
                val pattern = Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"")
                return pattern.find(jsonStr)?.groupValues?.get(1) ?: ""
            }

            val resultado = ResultadoCedula(
                valida = valida,
                cedula = extractField("cedula").ifBlank { cedula },
                nombreTitular = extractField("nombreTitular"),
                genero = extractField("genero"),
                institucion = extractField("institucion"),
                profesion = extractField("profesion"),
                entidad = extractField("entidad"),
                anoRegistro = extractField("anoRegistro"),
                mensaje = extractField("mensaje").ifBlank {
                    if (valida) "Cédula verificada con éxito" else "Cédula no encontrada en el Registro Nacional SEP"
                }
            )
            if (!deferred.isCompleted) deferred.complete(resultado)
        } catch (_: Throwable) {
            if (!deferred.isCompleted) {
                deferred.complete(ResultadoCedula(valida = false, mensaje = "Error al procesar la respuesta de la SEP"))
            }
        }
    }
}
