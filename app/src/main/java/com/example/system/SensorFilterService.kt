package com.example.system

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log

class SensorFilterService(private val context: Context, private val onStateChanged: (Boolean) -> Unit = {}) : SensorEventListener {

    companion object {
        private const val TAG = "SensorFilterService"
        @Volatile
        var isDeviceBlocked: Boolean = false
            private set
    }

    private var sensorManager: SensorManager? = null
    private var proximitySensor: Sensor? = null

    fun start() {
        try {
            sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            proximitySensor = sensorManager?.getDefaultSensor(Sensor.TYPE_PROXIMITY)

            if (proximitySensor != null) {
                sensorManager?.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL)
                Log.i(TAG, "Successfully registered Proximity sensor listener for pocket lock.")
            } else {
                Log.w(TAG, "Proximity sensor not available on this device.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error starting proximity lock listener", e)
        }
    }

    fun stop() {
        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {}
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = event.sensor.maximumRange
            val blocked = distance < maxRange
            if (isDeviceBlocked != blocked) {
                isDeviceBlocked = blocked
                Log.d(TAG, "Proximity lock state updated. Is obstructed: $blocked (Distance: $distance, Max: $maxRange)")
                onStateChanged(blocked)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
    }
}
