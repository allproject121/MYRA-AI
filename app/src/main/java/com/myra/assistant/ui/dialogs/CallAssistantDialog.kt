package com.myra.assistant.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.myra.assistant.R
import com.myra.assistant.viewmodel.MainViewModel

class CallAssistantDialog(
    context: Context,
    private val viewModel: MainViewModel,
    private val onSpeakRequest: (String) -> Unit,
    private val onNotice: (String) -> Unit
) : Dialog(context) {

    private val callers = listOf(
        Pair("Mummy", "+91 98765 43210"),
        Pair("Priya (Prime)", "+91 98765 43213"),
        Pair("Rahul Sharma", "+91 98765 43215")
    )
    private var callerIndex = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_call_assistant)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.94).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val closeBtn = findViewById<ImageButton>(R.id.dialogCloseBtn)
        val callerNameText = findViewById<TextView>(R.id.callerNameText)
        val callerNumberText = findViewById<TextView>(R.id.callerNumberText)
        val assistantVoicePromptText = findViewById<TextView>(R.id.assistantVoicePromptText)
        val btnAcceptCall = findViewById<Button>(R.id.btnAcceptCall)
        val btnRejectCall = findViewById<Button>(R.id.btnRejectCall)
        val btnTriggerSimulatedRing = findViewById<Button>(R.id.btnTriggerSimulatedRing)

        closeBtn?.setOnClickListener { dismiss() }

        fun updateCaller(idx: Int) {
            val caller = callers[idx % callers.size]
            callerNameText?.text = caller.first
            callerNumberText?.text = caller.second
            val speechPrompt = "Sir, ${caller.first} ka call aa raha hai. Uthau ya reject karu?"
            assistantVoicePromptText?.text = "MYRA: '$speechPrompt'"
            onSpeakRequest(speechPrompt)
        }

        updateCaller(callerIndex)

        btnAcceptCall?.setOnClickListener {
            viewModel.acceptCall()
            onNotice("Call accepted!")
            dismiss()
        }

        btnRejectCall?.setOnClickListener {
            viewModel.rejectCall()
            onNotice("Call rejected.")
            dismiss()
        }

        btnTriggerSimulatedRing?.setOnClickListener {
            callerIndex++
            updateCaller(callerIndex)
        }
    }
}
