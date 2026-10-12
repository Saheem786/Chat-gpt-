package com.example.jarvis

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.random.Random

class VoiceVisualizer(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val paint = Paint().apply {
        color = Color.parseColor("#00D9FF")
        strokeWidth = 10f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    
    private var amplitude = 0f

    fun setAmplitude(amp: Float) {
        amplitude = amp
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerY = height / 2f
        val barWidth = 20f
        val gap = 10f
        var x = 0f

        while (x < width) {
            val barHeight = (amplitude * height / 2f * Random.nextFloat()).coerceIn(10f, height.toFloat())
            canvas.drawLine(x, centerY - barHeight / 2, x, centerY + barHeight / 2, paint)
            x += barWidth + gap
        }
    }
}
