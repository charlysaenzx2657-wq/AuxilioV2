package com.example.touchswipe

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var tvShizukuStatus: TextView
    private lateinit var dotShizuku: View
    private lateinit var etPackage: EditText
    private lateinit var etX1: EditText
    private lateinit var etY1: EditText
    private lateinit var etX2: EditText
    private lateinit var etY2: EditText
    private lateinit var etDuration: EditText
    private lateinit var switchEnabled: Switch
    private lateinit var btnTestSwipe: Button

    private val permissionListener = Shizuku.OnRequestPermissionResultListener { _, grantResult ->
        updateShizukuStatus()
        val granted = grantResult == android.content.pm.PackageManager.PERMISSION_GRANTED
        Toast.makeText(
            this,
            if (granted) "Permiso de Shizuku concedido" else "Permiso de Shizuku denegado",
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvShizukuStatus = findViewById(R.id.tvShizukuStatus)
        dotShizuku = findViewById(R.id.dotShizuku)
        etPackage = findViewById(R.id.etPackage)
        etX1 = findViewById(R.id.etX1)
        etY1 = findViewById(R.id.etY1)
        etX2 = findViewById(R.id.etX2)
        etY2 = findViewById(R.id.etY2)
        etDuration = findViewById(R.id.etDuration)
        switchEnabled = findViewById(R.id.switchEnabled)
        btnTestSwipe = findViewById(R.id.btnTestSwipe)

        loadConfigIntoUi()
        animateCardsEntrance()

        findViewById<Button>(R.id.btnRequestShizukuPermission).setOnClickListener {
            requestShizukuPermission()
        }

        findViewById<Button>(R.id.btnOpenAccessibilitySettings).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnSaveConfig).setOnClickListener {
            saveConfigFromUi()
        }

        findViewById<Button>(R.id.btnActivar).setOnClickListener {
            activarServicio()
        }

        findViewById<Button>(R.id.btnTestSwipe).setOnClickListener {
            testSwipeNow()
        }

        switchEnabled.setOnCheckedChangeListener { _, isChecked ->
            SwipeConfig.setEnabled(this, isChecked)
        }

        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (e: Throwable) {
            // Shizuku no instalado / no corriendo
        }

        updateShizukuStatus()
    }

    override fun onResume() {
        super.onResume()
        updateShizukuStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Shizuku.removeRequestPermissionResultListener(permissionListener)
        } catch (e: Throwable) {
            // no-op
        }
    }

    private fun loadConfigIntoUi() {
        etPackage.setText(SwipeConfig.targetPackage(this))
        etX1.setText(SwipeConfig.x1(this).toString())
        etY1.setText(SwipeConfig.y1(this).toString())
        etX2.setText(SwipeConfig.x2(this).toString())
        etY2.setText(SwipeConfig.y2(this).toString())
        etDuration.setText(SwipeConfig.durationMs(this).toString())
        switchEnabled.isChecked = SwipeConfig.isEnabled(this)
    }

    private fun saveConfigFromUi() {
        val pkg = etPackage.text.toString().trim()
        if (pkg.isEmpty()) {
            Toast.makeText(this, "Ingresa un package válido", Toast.LENGTH_SHORT).show()
            return
        }

        SwipeConfig.save(
            context = this,
            targetPackage = pkg,
            x1 = etX1.text.toString().toIntOrNull() ?: 0,
            y1 = etY1.text.toString().toIntOrNull() ?: 0,
            x2 = etX2.text.toString().toIntOrNull() ?: 0,
            y2 = etY2.text.toString().toIntOrNull() ?: 0,
            durationMs = etDuration.text.toString().toIntOrNull() ?: 300
        )

        Toast.makeText(this, "Configuración guardada", Toast.LENGTH_SHORT).show()
    }

    private fun requestShizukuPermission() {
        if (!ShizukuGestureExecutor.isShizukuAvailable()) {
            Toast.makeText(this, "Shizuku no está corriendo", Toast.LENGTH_SHORT).show()
            return
        }
        if (ShizukuGestureExecutor.hasPermission()) {
            Toast.makeText(this, "Ya tienes permiso", Toast.LENGTH_SHORT).show()
            return
        }
        ShizukuGestureExecutor.requestPermission(REQUEST_CODE_SHIZUKU)
    }

    /**
     * Guarda la configuración actual y activa el servicio (equivalente a
     * encender el switch "Servicio activo"), para que el gesto empiece
     * a ejecutarse la próxima vez que se abra el package objetivo.
     */
    private fun activarServicio() {
        val pkg = etPackage.text.toString().trim()
        if (pkg.isEmpty()) {
            Toast.makeText(this, "Ingresa un package válido antes de activar", Toast.LENGTH_SHORT).show()
            return
        }
        if (!ShizukuGestureExecutor.isShizukuAvailable() || !ShizukuGestureExecutor.hasPermission()) {
            Toast.makeText(this, "⚠️ Shizuku no está listo, revisa el permiso", Toast.LENGTH_SHORT).show()
        }

        saveConfigFromUi()
        SwipeConfig.setEnabled(this, true)
        switchEnabled.isChecked = true

        Toast.makeText(this, "✅ Servicio activado", Toast.LENGTH_SHORT).show()
    }

    private fun testSwipeNow() {
        saveConfigFromUi()

        // Animación de pulso para dar feedback inmediato al tocar el botón
        val pulse = AnimationUtils.loadAnimation(this, R.anim.pulse)
        btnTestSwipe.startAnimation(pulse)

        scope.launch {
            val ok = withContextIo {
                ShizukuGestureExecutor.performSwipe(
                    SwipeConfig.x1(this@MainActivity),
                    SwipeConfig.y1(this@MainActivity),
                    SwipeConfig.x2(this@MainActivity),
                    SwipeConfig.y2(this@MainActivity),
                    SwipeConfig.durationMs(this@MainActivity)
                )
            }
            Toast.makeText(
                this@MainActivity,
                if (ok) "✅ Gesto ejecutado" else "⚠️ Falló el gesto (revisa Shizuku)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Anima la entrada de las tres cards con un fade + slide escalonado,
     * para que la pantalla se sienta más viva al abrir la app.
     */
    private fun animateCardsEntrance() {
        val cards = listOf<CardView>(
            findViewById(R.id.cardShizuku),
            findViewById(R.id.cardConfig),
            findViewById(R.id.cardService)
        )
        cards.forEachIndexed { index, card ->
            card.alpha = 0f
            card.postDelayed({
                val anim = AnimationUtils.loadAnimation(this, R.anim.fade_slide_up)
                card.alpha = 1f
                card.startAnimation(anim)
            }, index * 120L)
        }
    }

    /**
     * Actualiza el color del punto de estado según Shizuku:
     * amarillo = no disponible, rojo = disponible sin permiso, verde = listo.
     */
    private fun updateStatusDot(color: Int) {
        val drawable = dotShizuku.background as? GradientDrawable
        drawable?.setColor(ContextCompat.getColor(this, color))
    }

    private suspend fun <T> withContextIo(block: () -> T): T =
        kotlinx.coroutines.withContext(Dispatchers.IO) { block() }

    private fun updateShizukuStatus() {
        val available = ShizukuGestureExecutor.isShizukuAvailable()
        val granted = if (available) ShizukuGestureExecutor.hasPermission() else false

        tvShizukuStatus.text = when {
            !available -> "Shizuku: no disponible (¿está corriendo?)"
            !granted -> "Shizuku: disponible, permiso NO concedido"
            else -> "Shizuku: disponible y permiso concedido"
        }

        updateStatusDot(
            when {
                !available -> R.color.warning
                !granted -> R.color.error
                else -> R.color.success
            }
        )
    }

    companion object {
        private const val REQUEST_CODE_SHIZUKU = 1001
    }
}
