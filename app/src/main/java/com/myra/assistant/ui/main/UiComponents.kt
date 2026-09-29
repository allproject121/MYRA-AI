package com.myra.assistant.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myra.assistant.R
import java.util.Random

// 1. Data Model
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// 2. Chat Adapter
class ChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_MYRA = 2
    }

    private val messages = mutableListOf<ChatMessage>()

    fun addMessage(message: ChatMessage) {
        // Deduplication for MYRA messages
        if (!message.isUser && messages.isNotEmpty()) {
            val last = messages.last()
            if (!last.isUser && last.text.trim() == message.text.trim()) {
                return
            }
        }
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun lastMyraText(): String? {
        return messages.findLast { !it.isUser }?.text
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_MYRA
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val view = inflater.inflate(R.layout.item_chat_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_chat_myra, parent, false)
            MyraViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = messages[position]
        if (holder is UserViewHolder) {
            holder.text.text = msg.text
        } else if (holder is MyraViewHolder) {
            holder.text.text = msg.text
        }
    }

    override fun getItemCount(): Int = messages.size

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.chatUserText)
    }

    class MyraViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.chatMyraText)
    }
}

// 3. Waveform View
class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barCount = 20
    private val currentHeights = FloatArray(barCount) { 4f }
    private val targetHeights = FloatArray(barCount) { 4f }
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val random = Random()
    private var animator: ValueAnimator? = null
    private var baseAmplitude = 0f

    init {
        startAnimation()
    }

    fun startAnimation() {
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 50
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                updateHeights()
                invalidate()
            }
        }
        animator?.start()
    }

    fun stopAnimation() {
        animator?.cancel()
    }

    fun setAmplitude(rms: Float) {
        baseAmplitude = rms.coerceIn(0f, 1f)
    }

    private fun updateHeights() {
        val maxHeight = (height.toFloat() * 0.9f).coerceAtLeast(10f)
        for (i in 0 until barCount) {
            if (baseAmplitude > 0.05f) {
                val randFactor = 0.4f + random.nextFloat() * 0.6f
                targetHeights[i] = (baseAmplitude * maxHeight * randFactor).coerceIn(4f, maxHeight)
            } else {
                targetHeights[i] = 4f
            }
            // Lerp towards target
            currentHeights[i] += (targetHeights[i] - currentHeights[i]) * 0.3f
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width == 0 || height == 0) return

        val totalSpacing = width.toFloat()
        val barWidth = (totalSpacing / barCount) * 0.55f
        val gap = (totalSpacing - (barWidth * barCount)) / (barCount + 1)
        val cy = height / 2f

        for (i in 0 until barCount) {
            val barH = currentHeights[i]
            val left = gap + i * (barWidth + gap)
            val top = cy - (barH / 2f)
            val right = left + barWidth
            val bottom = cy + (barH / 2f)

            val alpha = (150 + (barH / (height * 0.9f) * 105)).toInt().coerceIn(150, 255)
            paint.color = Color.argb(alpha, 0xFF, 0x17, 0x44)
            canvas.drawRoundRect(RectF(left, top, right, bottom), barWidth / 2f, barWidth / 2f, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopAnimation()
    }
}
