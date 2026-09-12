package com.example.touchswipe

import android.content.Context

/**
 * Guarda y expone la configuración del gesto: package objetivo,
 * coordenadas de inicio/fin y duración del swipe.
 */
object SwipeConfig {

    private const val PREFS = "touch_swipe_prefs"

    private const val KEY_PACKAGE = "target_package"
    private const val KEY_X1 = "x1"
    private const val KEY_Y1 = "y1"
    private const val KEY_X2 = "x2"
    private const val KEY_Y2 = "y2"
    private const val KEY_DURATION = "duration_ms"
    private const val KEY_ENABLED = "service_enabled"

    fun save(
        context: Context,
        targetPackage: String,
        x1: Int, y1: Int,
        x2: Int, y2: Int,
        durationMs: Int
    ) {
        prefs(context).edit()
            .putString(KEY_PACKAGE, targetPackage)
            .putInt(KEY_X1, x1)
            .putInt(KEY_Y1, y1)
            .putInt(KEY_X2, x2)
            .putInt(KEY_Y2, y2)
            .putInt(KEY_DURATION, durationMs)
            .apply()
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        prefs(context).edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    fun isEnabled(context: Context): Boolean =
        prefs(context).getBoolean(KEY_ENABLED, false)

    fun targetPackage(context: Context): String =
        prefs(context).getString(KEY_PACKAGE, "") ?: ""

    fun x1(context: Context) = prefs(context).getInt(KEY_X1, 0)
    fun y1(context: Context) = prefs(context).getInt(KEY_Y1, 0)
    fun x2(context: Context) = prefs(context).getInt(KEY_X2, 0)
    fun y2(context: Context) = prefs(context).getInt(KEY_Y2, 0)
    fun durationMs(context: Context) = prefs(context).getInt(KEY_DURATION, 300)

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
