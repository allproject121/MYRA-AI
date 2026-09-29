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
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import com.myra.assistant.R
import com.myra.assistant.model.AppCommand
import com.myra.assistant.viewmodel.MainViewModel

class SocialMediaAgentDialog(
    context: Context,
    private val viewModel: MainViewModel,
    private val onNotice: (String) -> Unit
) : Dialog(context) {

    private val smartCaptions = listOf(
        "Chasing dreams and building the future with MYRA AI 🚀✨ #AICompanion #TechVibes #Innovation2026",
        "Golden hour thoughts with my favorite people 🌅❤️ #SunsetVibes #MomentsThatMatter",
        "Focus on the vision, execute with passion 💼⚡ #HustleMode #Productivity #AIWorkflow",
        "Life is better when you're laughing with good company 😊✨ #GoodVibesOnly #WeekendVibes"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_social_media_agent)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.94).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val closeBtn = findViewById<ImageButton>(R.id.dialogCloseBtn)
        val radioInstagram = findViewById<RadioButton>(R.id.radioInstagram)
        val radioStory = findViewById<RadioButton>(R.id.radioStory)
        val radioReel = findViewById<RadioButton>(R.id.radioReel)
        val socialCaptionField = findViewById<EditText>(R.id.socialCaptionField)
        val btnSuggestCaption = findViewById<Button>(R.id.btnSuggestCaption)
        val btnConfirmPublish = findViewById<Button>(R.id.btnConfirmPublish)
        val btnRejectPublish = findViewById<Button>(R.id.btnRejectPublish)
        val gateStatusText = findViewById<TextView>(R.id.gateStatusText)

        closeBtn?.setOnClickListener { dismiss() }

        btnSuggestCaption?.setOnClickListener {
            val randomCaption = smartCaptions.random()
            socialCaptionField?.setText(randomCaption)
            onNotice("AI Caption generated!")
        }

        btnConfirmPublish?.setOnClickListener {
            val platform = if (radioInstagram?.isChecked == true) "INSTAGRAM" else "FACEBOOK"
            val action = if (radioStory?.isChecked == true) "POST_STORY" else if (radioReel?.isChecked == true) "POST_REEL" else "POST_FEED"
            val caption = socialCaptionField?.text?.toString()?.trim() ?: "Shared via MYRA"

            viewModel.executeCommand(AppCommand(
                AppCommand.TYPE_SOCIAL_MEDIA_TASK,
                mapOf(
                    "platform" to platform,
                    "action" to action,
                    "caption" to caption
                )
            ))
            onNotice("✅ Confirmed! Post dispatched to $platform.")
            dismiss()
        }

        btnRejectPublish?.setOnClickListener {
            viewModel.executeCommand(AppCommand(
                AppCommand.TYPE_SOCIAL_MEDIA_CONTROL,
                mapOf("command" to "reject")
            ))
            gateStatusText?.text = "Post cancelled safely. Publication aborted."
            onNotice("Social post discarded safely.")
            dismiss()
        }
    }
}
