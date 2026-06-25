package com.airdrums

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Handler
import android.os.Looper
import kotlin.math.abs

class AccelerometerManager(
    context: Context,
    private val onBassDrum: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val mainHandler = Handler(Looper.getMainLooper())

    var threshold: Float = 18f       // m/s² delta above gravity — tunable via UI
    private val cooldownMs = 150L
    private var lastTriggerMs = 0L

    val isAvailable: Boolean get() = accelerometer != null

    fun register() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun unregister() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        // Z axis: perpendicular to screen surface. When phone lies flat on a leg,
        // a downward tap produces a sharp spike in Z above/below gravity (9.8 m/s²).
        val az = event.values[2]
        val delta = abs(az - SensorManager.GRAVITY_EARTH)
        val now = System.currentTimeMillis()

        if (delta > threshold && (now - lastTriggerMs) > cooldownMs) {
            lastTriggerMs = now
            mainHandler.post { onBassDrum() }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}
