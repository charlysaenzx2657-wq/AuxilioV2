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
     *
     * NOTA: Shizuku.newProcess es un método interno (no público) de la
     * librería dev.rikka.shizuku:api, así que se invoca por reflexión,
     * que es la forma estándar en la que lo hacen la mayoría de apps
     * basadas en Shizuku.
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

            val process = newProcessViaReflection(command, null, null)
            process.waitFor()
            val exitCode = process.exitValue() as Int
            Log.d(TAG, "Swipe ejecutado, exit code=$exitCode")
            exitCode == 0
        } catch (e: Throwable) {
            Log.e(TAG, "Error ejecutando swipe vía Shizuku", e)
            false
        }
    }

    /**
     * Invoca el método privado Shizuku.newProcess(String[], String[], String)
     * por reflexión, ya que no está expuesto públicamente en la API.
     * Devuelve un objeto rikka.shizuku.ShizukuRemoteProcess sobre el que
     * llamamos waitFor()/exitValue() también por reflexión.
     */
    private fun newProcessViaReflection(cmd: Array<String>, env: Array<String>?, dir: String?): ReflectedProcess {
        val method = Shizuku::class.java.getDeclaredMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        method.isAccessible = true
        val processObj = method.invoke(null, cmd, env, dir)
            ?: throw IllegalStateException("Shizuku.newProcess devolvió null")
        return ReflectedProcess(processObj)
    }

    /**
     * Wrapper mínimo para llamar waitFor()/exitValue() sobre el objeto
     * ShizukuRemoteProcess obtenido por reflexión, sin depender de su tipo
     * concreto en tiempo de compilación.
     */
    private class ReflectedProcess(private val target: Any) {
        fun waitFor(): Int {
            val method = target.javaClass.getMethod("waitFor")
            return method.invoke(target) as Int
        }

        fun exitValue(): Any {
            val method = target.javaClass.getMethod("exitValue")
            return method.invoke(target)
        }
    }
}
