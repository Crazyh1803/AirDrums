package com.airdrums

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.View
import androidx.core.graphics.ColorUtils

class DrumPadView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var onDrumHit: ((DrumEngine.Drum) -> Unit)? = null

    private data class Zone(
        val drum: DrumEngine.Drum,
        val label: String,
        val color: Int,
        val cx: Float,
        val cy: Float,
        val radius: Float
    )

    private data class Flash(val zone: Zone, var alpha: Float = 1f)

    private val zones = mutableListOf<Zone>()
    private val flashes = mutableListOf<Flash>()
    private var bassFlashAlpha = 0f

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.MONOSPACE
    }
    private val bgPaint = Paint().apply { color = Color.parseColor("#1A1A2E") }

    // Zone layout: (cx fraction, cy fraction, radius fraction, drum, label, color)
    private val zoneSpecs = listOf(
        Triple(0.25f to 0.18f, 0.115f, Triple(DrumEngine.Drum.HI_HAT, "HI-HAT", Color.parseColor("#00BFFF"))),
        Triple(0.75f to 0.18f, 0.115f, Triple(DrumEngine.Drum.CRASH,  "CRASH",  Color.parseColor("#FFD700"))),
        Triple(0.22f to 0.47f, 0.125f, Triple(DrumEngine.Drum.SNARE,  "SNARE",  Color.parseColor("#FF4444"))),
        Triple(0.55f to 0.44f, 0.105f, Triple(DrumEngine.Drum.TOM1,   "TOM 1",  Color.parseColor("#00FF88"))),
        Triple(0.83f to 0.47f, 0.105f, Triple(DrumEngine.Drum.TOM2,   "TOM 2",  Color.parseColor("#FF00AA"))),
        Triple(0.50f to 0.73f, 0.12f,  Triple(DrumEngine.Drum.RIDE,   "RIDE",   Color.parseColor("#FFFFFF")))
    )

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val minDim = minOf(w, h).toFloat()
        zones.clear()
        for ((pos, rFrac, info) in zoneSpecs) {
            zones += Zone(
                drum   = info.first,
                label  = info.second,
                color  = info.third,
                cx     = pos.first * w,
                cy     = pos.second * h,
                radius = rFrac * minDim
            )
        }
        textPaint.textSize = minDim * 0.038f
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.actionMasked
        if (action == ACTION_DOWN || action == ACTION_POINTER_DOWN) {
            val idx = event.actionIndex
            hitTest(event.getX(idx), event.getY(idx))
        }
        return true
    }

    private fun hitTest(x: Float, y: Float) {
        for (zone in zones) {
            val dx = x - zone.cx
            val dy = y - zone.cy
            if (dx * dx + dy * dy <= zone.radius * zone.radius) {
                onDrumHit?.invoke(zone.drum)
                triggerFlash(zone)
                break
            }
        }
    }

    private fun triggerFlash(zone: Zone) {
        flashes.removeAll { it.zone.drum == zone.drum }
        flashes += Flash(zone)
        invalidate()
    }

    fun triggerBassFlash() {
        bassFlashAlpha = 1f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // Bass drum flash — full-screen tint
        if (bassFlashAlpha > 0.01f) {
            fillPaint.color = ColorUtils.setAlphaComponent(Color.parseColor("#FF4500"), (bassFlashAlpha * 60).toInt())
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fillPaint)
            bassFlashAlpha *= 0.7f
            invalidate()
        }

        // Drum pads
        val iterator = flashes.iterator()
        val deadFlashes = mutableListOf<Flash>()

        for (zone in zones) {
            val flash = flashes.find { it.zone.drum == zone.drum }

            // Dim base fill
            fillPaint.color = ColorUtils.setAlphaComponent(zone.color, 55)
            canvas.drawCircle(zone.cx, zone.cy, zone.radius, fillPaint)

            // Ring
            ringPaint.color = ColorUtils.setAlphaComponent(zone.color, 180)
            canvas.drawCircle(zone.cx, zone.cy, zone.radius, ringPaint)

            // Hit flash overlay
            if (flash != null) {
                fillPaint.color = ColorUtils.setAlphaComponent(zone.color, (flash.alpha * 210).toInt())
                canvas.drawCircle(zone.cx, zone.cy, zone.radius, fillPaint)
                flash.alpha *= 0.72f
                if (flash.alpha < 0.02f) deadFlashes += flash
            }

            // Label
            textPaint.color = ColorUtils.setAlphaComponent(Color.WHITE,
                if (flash != null) (100 + flash.alpha * 155).toInt() else 140)
            canvas.drawText(zone.label, zone.cx, zone.cy + textPaint.textSize * 0.38f, textPaint)
        }

        flashes.removeAll(deadFlashes)
        if (flashes.isNotEmpty()) invalidate()

        // "Place on leg" hint — shown at bottom if no bass flash recently
        if (bassFlashAlpha <= 0.01f) {
            textPaint.color = Color.parseColor("#33FFFFFF")
            textPaint.textSize = minOf(width, height) * 0.028f
            canvas.drawText("place phone on leg for bass", width / 2f, height * 0.92f, textPaint)
            textPaint.textSize = minOf(width, height) * 0.038f
        }
    }
}
