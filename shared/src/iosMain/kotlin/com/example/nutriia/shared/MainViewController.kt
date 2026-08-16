@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

package com.example.nutriia.shared

import androidx.compose.ui.window.ComposeUIViewController
import platform.Foundation.NSLog
import platform.UIKit.UIViewController
import kotlin.native.setUnhandledExceptionHook

private val hookInstalled: Boolean by lazy {
    setUnhandledExceptionHook { throwable ->
        val crashMsg = buildString {
            appendLine("════════════════════════════════════════════════════════════════════")
            appendLine("💥💥💥 KOTLIN/NATIVE UNHANDLED EXCEPTION CAUGHT 💥💥💥")
            appendLine("Tipo: ${throwable::class.qualifiedName ?: throwable::class.simpleName}")
            appendLine("Mensaje: ${throwable.message}")
            appendLine("Causa: ${throwable.cause?.message}")
            appendLine("Stack Trace:")
            appendLine(throwable.stackTraceToString())
            appendLine("════════════════════════════════════════════════════════════════════")
        }
        NSLog("%s", crashMsg)
        println(crashMsg)
        com.example.nutriia.platform.CrashStorage.saveCrash(crashMsg)
    }
    true
}

fun MainViewController(): UIViewController {
    hookInstalled
    NSLog("🚀 [NutriIA-MainVC] MainViewController inicializado. Creando ComposeUIViewController...")
    return ComposeUIViewController(
        configure = {
            enforceStrictPlistSanityCheck = false
        }
    ) {
        AppiOS()
    }
}

