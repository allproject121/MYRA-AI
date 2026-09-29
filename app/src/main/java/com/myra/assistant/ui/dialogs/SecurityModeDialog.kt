package com.myra.assistant.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import com.myra.assistant.R

class SecurityModeDialog(
    context: Context,
    private val onIntruderAlertTriggered: () -> Unit,
    private val onPinVerified: (Boolean) -> Unit
) : Dialog(context) {

    private var isSentryArmed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_security_mode)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.94).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val closeBtn = findViewById<ImageButton>(R.id.dialogCloseBtn)
        val sentryStatusText = findViewById<TextView>(R.id.sentryStatusText)
        val btnToggleSentry = findViewById<Button>(R.id.btnToggleSentry)
        val btnSimulateIntruder = findViewById<Button>(R.id.btnSimulateIntruder)
        val pinInputField = findViewById<EditText>(R.id.pinInputField)
        val btnVerifyPin = findViewById<Button>(R.id.btnVerifyPin)
        val pinResultText = findViewById<TextView>(R.id.pinResultText)

        closeBtn?.setOnClickListener { dismiss() }

        btnToggleSentry?.setOnClickListener {
            isSentryArmed = !isSentryArmed
            if (isSentryArmed) {
                sentryStatusText?.text = "SENTRY: ARMED & WATCHING 🛡️"
                sentryStatusText?.setTextColor(context.getColor(R.color.status_green))
                btnToggleSentry.text = "DISARM"
            } else {
                sentryStatusText?.text = "SENTRY: INACTIVE"
                sentryStatusText?.setTextColor(context.getColor(R.color.text_secondary))
                btnToggleSentry.text = "ARM SENTRY"
            }
        }

        btnSimulateIntruder?.setOnClickListener {
            onIntruderAlertTriggered()
            dismiss()
        }

        btnVerifyPin?.setOnClickListener {
            val entered = pinInputField?.text?.toString()?.trim() ?: ""
            val prefs = context.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
            val storedPin = prefs.getString("security_pin", "2601") ?: "2601"

            if (entered == storedPin) {
                pinResultText?.text = "✅ Access Granted! Device unlocked."
                pinResultText?.setTextColor(context.getColor(R.color.status_green))
                onPinVerified(true)
            } else {
                pinResultText?.text = "❌ Incorrect PIN! Try again."
                pinResultText?.setTextColor(context.getColor(R.color.primary_red))
                onPinVerified(false)
            }
        }
    }
}
