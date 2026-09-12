package com.example.touchswipe

import android.util.Log
import rikka.shizuku.Shizuku

/**
 * Ejecuta un gesto de swipe usando el proceso "shell" que expone Shizuku.
 * Esto equivale a correr:
 *   input touchscreen swipe x1 y1 x2 y2 duration
 * pero con los permisos de shell que Shizuku concede a la app,
 * sin requerir que el dispositivo esté rooteado.
 */
object ShizukuGestureExecutor {

    private const val TAG = "ShizukuGestureExecutor"

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }
    }

    fun hasPermission(): Boolean {
        return try {
            Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
        } catch (e: Throwable) {
            false
        }
    }

    fun requestPermission(requestCode: Int) {
        try {
            Shizuku.requestPermission(requestCode)
        } catch (e: Throwable) {
            Log.e(TAG, "No se pudo solicitar permiso de Shizuku", e)
        }
    }

    /**
     * Lanza el swipe. Devuelve true si el proceso se pudo iniciar
     * (no garantiza que el gesto haya sido "aceptado" por la UI destino,
     * solo que el comando shell se ejecutó).
     */
    fun performSwipe(x1: Int, y1: Int, x2: Int, y2: Int, durationMs: Int): Boolean {
        if (!isShizukuAvailable() || !hasPermission()) {
            Log.w(TAG, "Shizuku no disponible o sin permiso")
            return false
        }

        return try {
            val command = arrayOf(
                "input", "touchscreen", "swipe",
                x1.toString(), y1.toString(),
                x2.toString(), y2.toString(),
                durationMs.toString()
            )

            val process = Shizuku.newProcess(command, null, null)
            process.waitFor()
            val exitCode = process.exitValue()
            Log.d(TAG, "Swipe ejecutado, exit code=$exitCode")
            exitCode == 0
        } catch (e: Throwable) {
            Log.e(TAG, "Error ejecutando swipe vía Shizuku", e)
            false
        }
    }
}
