package com.example.touchswipe

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Servicio de accesibilidad "ligero": no lee contenido de pantalla,
 * solo escucha eventos de cambio de ventana (TYPE_WINDOW_STATE_CHANGED)
 * para saber qué package pasó a primer plano.
 *
 * Cuando detecta que el package configurado se abrió, ejecuta el gesto
 * UNA sola vez por apertura (hasta que el usuario salga de la app y
 * vuelva a entrar).
 */
class ForegroundWatcherService : AccessibilityService() {

    private var lastPackage: String? = null
    private var gestureFiredForCurrentSession = false
    private val scope = CoroutineScope(Dispatchers.Default)

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Servicio de accesibilidad conectado")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val packageName = event.packageName?.toString() ?: return

        // Solo actuamos si el servicio está activado desde la UI
        if (!SwipeConfig.isEnabled(applicationContext)) return

        val target = SwipeConfig.targetPackage(applicationContext)
        if (target.isBlank()) return

        // Si cambiamos de app, reseteamos el flag de "ya disparado"
        if (packageName != lastPackage) {
            lastPackage = packageName
            gestureFiredForCurrentSession = false
        }

        if (packageName == target && !gestureFiredForCurrentSession) {
            gestureFiredForCurrentSession = true
            fireGesture()
        }
    }

    private fun fireGesture() {
        val x1 = SwipeConfig.x1(applicationContext)
        val y1 = SwipeConfig.y1(applicationContext)
        val x2 = SwipeConfig.x2(applicationContext)
        val y2 = SwipeConfig.y2(applicationContext)
        val duration = SwipeConfig.durationMs(applicationContext)

        scope.launch {
            val ok = ShizukuGestureExecutor.performSwipe(x1, y1, x2, y2, duration)
            Log.d(TAG, "Gesto disparado para package objetivo, éxito=$ok")
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "Servicio de accesibilidad interrumpido")
    }
}

private const val TAG = "ForegroundWatcher"
