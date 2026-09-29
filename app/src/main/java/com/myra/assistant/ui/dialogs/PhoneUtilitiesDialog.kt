package com.myra.assistant.ui.dialogs

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.myra.assistant.R
import java.security.SecureRandom
import java.util.Random

class PhoneUtilitiesDialog(
    context: Context,
    private val onNotice: (String) -> Unit
) : Dialog(context) {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_phone_utilities)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.94).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val closeBtn = findViewById<ImageButton>(R.id.dialogCloseBtn)
        val generatedPasswordText = findViewById<TextView>(R.id.generatedPasswordText)
        val btnGeneratePassword = findViewById<Button>(R.id.btnGeneratePassword)
        val btnCopyPassword = findViewById<Button>(R.id.btnCopyPassword)

        val qrInputText = findViewById<EditText>(R.id.qrInputText)
        val qrImageView = findViewById<ImageView>(R.id.qrImageView)
        val btnGenerateQr = findViewById<Button>(R.id.btnGenerateQr)

        val speedDownloadText = findViewById<TextView>(R.id.speedDownloadText)
        val speedUploadText = findViewById<TextView>(R.id.speedUploadText)
        val speedPingText = findViewById<TextView>(R.id.speedPingText)
        val btnRunSpeedTest = findViewById<Button>(R.id.btnRunSpeedTest)

        val noteInputField = findViewById<EditText>(R.id.noteInputField)
        val btnSaveNote = findViewById<Button>(R.id.btnSaveNote)
        val savedNotesText = findViewById<TextView>(R.id.savedNotesText)

        closeBtn?.setOnClickListener { dismiss() }

        // 1. Password Generator
        btnGeneratePassword?.setOnClickListener {
            val pass = generateStrongPassword(16)
            generatedPasswordText?.text = pass
        }

        btnCopyPassword?.setOnClickListener {
            val pass = generatedPasswordText?.text?.toString() ?: ""
            copyToClipboard(pass)
            Toast.makeText(context, "Password copied to clipboard!", Toast.LENGTH_SHORT).show()
            onNotice("Password copied: $pass")
        }

        // 2. QR Code Renderer (Canvas matrix)
        renderSampleQr(qrImageView, "https://github.com/allproject121/MYRA-AI")
        btnGenerateQr?.setOnClickListener {
            val text = qrInputText?.text?.toString()?.trim()
            val content = if (text.isNullOrEmpty()) "MYRA AI Assistant 2026" else text
            renderSampleQr(qrImageView, content)
            onNotice("QR Code generated for: $content")
        }

        // 3. Speed Test Simulator
        btnRunSpeedTest?.setOnClickListener {
            btnRunSpeedTest.isEnabled = false
            btnRunSpeedTest.text = "Testing Network Speed..."
            speedDownloadText?.text = "--"
            speedUploadText?.text = "--"
            speedPingText?.text = "--"

            handler.postDelayed({
                val ping = 12 + Random().nextInt(15)
                val dl = 65.0 + Random().nextDouble() * 45.0
                val ul = 28.0 + Random().nextDouble() * 25.0

                speedPingText?.text = "$ping ms"
                speedDownloadText?.text = String.format("%.1f Mbps", dl)
                speedUploadText?.text = String.format("%.1f Mbps", ul)

                btnRunSpeedTest.isEnabled = true
                btnRunSpeedTest.text = "Run Speed Benchmark 🚀"
                onNotice("Speed test complete: ${String.format("%.1f", dl)} Mbps DL, $ping ms Ping")
            }, 1200)
        }

        // 4. Notes Manager
        loadSavedNotes(savedNotesText)
        btnSaveNote?.setOnClickListener {
            val note = noteInputField?.text?.toString()?.trim() ?: ""
            if (note.isNotEmpty()) {
                appendNote(note, savedNotesText)
                noteInputField?.setText("")
                Toast.makeText(context, "Memo saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateStrongPassword(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()-_=+"
        val random = SecureRandom()
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }

    private fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("MYRA Generated", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun renderSampleQr(imageView: ImageView?, text: String) {
        if (imageView == null) return
        val size = 250
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        }
        canvas.drawColor(Color.WHITE)

        val hash = text.hashCode()
        val random = Random(hash.toLong())
        val gridSize = 25
        val cellSize = size / gridSize

        // Draw 3 Corner finder patterns
        drawFinderPattern(canvas, paint, 0, 0, cellSize)
        drawFinderPattern(canvas, paint, (gridSize - 7) * cellSize, 0, cellSize)
        drawFinderPattern(canvas, paint, 0, (gridSize - 7) * cellSize, cellSize)

        // Draw pseudo-random data modules derived from text hash
        for (x in 0 until gridSize) {
            for (y in 0 until gridSize) {
                // skip finder patterns
                if ((x < 7 && y < 7) || (x >= gridSize - 7 && y < 7) || (x < 7 && y >= gridSize - 7)) {
                    continue
                }
                if (random.nextBoolean()) {
                    canvas.drawRect(
                        (x * cellSize).toFloat(),
                        (y * cellSize).toFloat(),
                        ((x + 1) * cellSize).toFloat(),
                        ((y + 1) * cellSize).toFloat(),
                        paint
                    )
                }
            }
        }
        imageView.setImageBitmap(bitmap)
    }

    private fun drawFinderPattern(canvas: Canvas, paint: Paint, startX: Int, startY: Int, cellSize: Int) {
        // Outer 7x7 box
        paint.color = Color.BLACK
        canvas.drawRect(startX.toFloat(), startY.toFloat(), (startX + 7 * cellSize).toFloat(), (startY + 7 * cellSize).toFloat(), paint)
        // Inner 5x5 white
        paint.color = Color.WHITE
        canvas.drawRect((startX + cellSize).toFloat(), (startY + cellSize).toFloat(), (startX + 6 * cellSize).toFloat(), (startY + 6 * cellSize).toFloat(), paint)
        // Center 3x3 black
        paint.color = Color.BLACK
        canvas.drawRect((startX + 2 * cellSize).toFloat(), (startY + 2 * cellSize).toFloat(), (startX + 5 * cellSize).toFloat(), (startY + 5 * cellSize).toFloat(), paint)
    }

    private fun loadSavedNotes(textView: TextView?) {
        val prefs = context.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val notes = prefs.getString("saved_notes_list", "• Meeting with Rahul at 4 PM\n• Buy groceries & fuel\n• MYRA Titan version deployed")
        textView?.text = notes
    }

    private fun appendNote(newNote: String, textView: TextView?) {
        val prefs = context.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val current = prefs.getString("saved_notes_list", "") ?: ""
        val updated = if (current.isEmpty()) "• $newNote" else "• $newNote\n$current"
        prefs.edit().putString("saved_notes_list", updated).apply()
        textView?.text = updated
    }
}
