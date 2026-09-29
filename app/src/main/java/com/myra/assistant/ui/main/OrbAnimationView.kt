package com.myra.assistant.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.cos
import kotlin.math.sin

class OrbAnimationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class OrbState {
        IDLE,
        LISTENING,
        SPEAKING,
        THINKING,
        ACTIVE
    }

    private var currentState: OrbState = OrbState.IDLE
    private var scaleMultiplier = 1f
    private var glowAlpha = 180
    private var rotationAngle = 0f
    private var waveOffset = 0f
    private var thinkingAngle = 0f
    private var amplitude = 0f

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val thinkingPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 5f
        strokeCap = Paint.Cap.ROUND
    }
    private val particlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private var pulseAnimator: ValueAnimator? = null
    private var rotationAnimator: ValueAnimator? = null
    private var thinkingAnimator: ValueAnimator? = null

    init {
        startAnimators()
    }

    private fun startAnimators() {
        pulseAnimator = ValueAnimator.ofFloat(1f, 1.15f, 1f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                scaleMultiplier = it.animatedValue as Float
                glowAlpha = (120 + (scaleMultiplier - 1f) * (100 / 0.15f)).toInt().coerceIn(120, 220)
                invalidate()
            }
        }
        pulseAnimator?.start()

        rotationAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 4000
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                rotationAngle = it.animatedValue as Float
                waveOffset += 0.1f
                invalidate()
            }
        }
        rotationAnimator?.start()

        thinkingAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                thinkingAngle = it.animatedValue as Float
                if (currentState == OrbState.THINKING) {
                    invalidate()
                }
            }
        }
        thinkingAnimator?.start()
    }

    fun setState(state: OrbState) {
        if (currentState != state) {
            currentState = state
            invalidate()
        }
    }

    fun setAmplitude(amp: Float) {
        amplitude = amp.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val cx = width / 2f
        val cy = height / 2f
        val baseRadius = (width.coerceAtMost(height) / 2f) * 0.42f
        val radius = baseRadius * (if (currentState == OrbState.IDLE) scaleMultiplier else (1f + amplitude * 0.25f))

        // State-based Colors
        val (coreStart, coreEnd) = when (currentState) {
            OrbState.IDLE -> Pair(Color.parseColor("#B71C1C"), Color.parseColor("#880E4F"))
            OrbState.LISTENING, OrbState.ACTIVE -> Pair(Color.parseColor("#FF1744"), Color.parseColor("#D500F9"))
            OrbState.SPEAKING -> Pair(Color.parseColor("#E040FB"), Color.parseColor("#FF1744"))
            OrbState.THINKING -> Pair(Color.parseColor("#40C4FF"), Color.parseColor("#00B0FF"))
        }

        // Layer 1: Radial Glow
        paint.shader = RadialGradient(
            cx, cy, radius * 1.6f,
            intArrayOf(adjustAlpha(coreStart, glowAlpha), adjustAlpha(coreEnd, 40), Color.TRANSPARENT),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius * 1.6f, paint)

        // Layer 2: Core Sphere
        paint.shader = RadialGradient(
            cx - radius * 0.2f, cy - radius * 0.25f, radius * 1.1f,
            intArrayOf(coreStart, coreEnd, Color.parseColor("#150005")),
            floatArrayOf(0f, 0.65f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, radius, paint)

        // Layer 3: Rotating Dashed Rings
        if (currentState != OrbState.IDLE) {
            ringPaint.color = adjustAlpha(coreStart, 160)
            ringPaint.pathEffect = DashPathEffect(floatArrayOf(20f, 12f), waveOffset * 10f)
            val ringBounds = RectF(cx - radius * 1.25f, cy - radius * 1.25f, cx + radius * 1.25f, cy + radius * 1.25f)
            canvas.save()
            canvas.rotate(rotationAngle, cx, cy)
            canvas.drawOval(ringBounds, ringPaint)
            canvas.restore()

            canvas.save()
            canvas.rotate(-rotationAngle * 0.7f, cx, cy)
            val innerRing = RectF(cx - radius * 1.15f, cy - radius * 1.15f, cx + radius * 1.15f, cy + radius * 1.15f)
            canvas.drawOval(innerRing, ringPaint)
            canvas.restore()
        }

        // Layer 4: Wave Rings (Sine wave)
        if (currentState == OrbState.SPEAKING || currentState == OrbState.LISTENING) {
            ringPaint.pathEffect = null
            ringPaint.color = adjustAlpha(coreEnd, 120)
            val waveR = radius * (1.1f + amplitude * 0.2f)
            canvas.drawCircle(cx, cy, waveR, ringPaint)
        }

        // Layer 5: Thinking Spin Arc
        if (currentState == OrbState.THINKING) {
            thinkingPaint.color = Color.parseColor("#00B0FF")
            val thinkRect = RectF(cx - radius * 1.3f, cy - radius * 1.3f, cx + radius * 1.3f, cy + radius * 1.3f)
            canvas.drawArc(thinkRect, thinkingAngle, 100f, false, thinkingPaint)
            canvas.drawArc(thinkRect, thinkingAngle + 180f, 60f, false, thinkingPaint)
        }

        // Layer 6: Orbiting Particles (12 dots)
        if (currentState == OrbState.ACTIVE || currentState == OrbState.SPEAKING) {
            particlePaint.color = Color.parseColor("#E0E7FF")
            val particleCount = 12
            for (i in 0 until particleCount) {
                val angle = Math.toRadians((rotationAngle * 1.5 + i * (360.0 / particleCount)))
                val dist = radius * (1.35f + sin(waveOffset + i).toFloat() * 0.15f)
                val px = cx + (dist * cos(angle)).toFloat()
                val py = cy + (dist * sin(angle)).toFloat()
                canvas.drawCircle(px, py, 3f + (amplitude * 3f), particlePaint)
            }
        }

        // Layer 7: Inner Highlight Specular
        paint.shader = RadialGradient(
            cx - radius * 0.35f, cy - radius * 0.35f, radius * 0.45f,
            intArrayOf(Color.parseColor("#80FFFFFF"), Color.TRANSPARENT),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx - radius * 0.35f, cy - radius * 0.35f, radius * 0.45f, paint)
    }

    private fun adjustAlpha(color: Int, alpha: Int): Int {
        return Color.argb(alpha.coerceIn(0, 255), Color.red(color), Color.green(color), Color.blue(color))
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pulseAnimator?.cancel()
        rotationAnimator?.cancel()
        thinkingAnimator?.cancel()
    }
}
