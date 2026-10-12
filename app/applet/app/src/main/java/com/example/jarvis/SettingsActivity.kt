package com.example.jarvis

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private var speechRate = 1.0f
    private var speechPitch = 1.0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = getSharedPreferences("JarvisPrefs", Context.MODE_PRIVATE)
        speechRate = prefs.getFloat("rate", 1.0f)
        speechPitch = prefs.getFloat("pitch", 1.0f)

        val seekRate = findViewById<SeekBar>(R.id.seekRate)
        val seekPitch = findViewById<SeekBar>(R.id.seekPitch)
        val btnSave = findViewById<Button>(R.id.btnSave)

        seekRate.progress = ((speechRate - 0.1f) * 10).toInt()
        seekPitch.progress = ((speechPitch - 0.1f) * 10).toInt()

        btnSave.setOnClickListener {
            speechRate = (seekRate.progress / 10f) + 0.1f
            speechPitch = (seekPitch.progress / 10f) + 0.1f
            prefs.edit().putFloat("rate", speechRate).putFloat("pitch", speechPitch).apply()
            finish()
        }
    }
}
