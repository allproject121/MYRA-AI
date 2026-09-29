package com.myra.assistant.ui.dialogs

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.myra.assistant.R
import com.myra.assistant.model.AppCommand
import com.myra.assistant.viewmodel.MainViewModel
import org.json.JSONArray

class ToolsActionCenterDialog(
    context: Context,
    private val viewModel: MainViewModel,
    private val onActionTriggered: (String) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_tools_action_center)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            (context.resources.displayMetrics.widthPixels * 0.94).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val closeBtn = findViewById<ImageButton>(R.id.dialogCloseBtn)
        val btnTriggerSos = findViewById<Button>(R.id.btnTriggerSos)
        val btnActionTorch = findViewById<android.view.View>(R.id.btnActionTorch)
        val torchStateText = findViewById<TextView>(R.id.torchStateText)
        val btnActionBattery = findViewById<android.view.View>(R.id.btnActionBattery)
        val btnActionWifi = findViewById<android.view.View>(R.id.btnActionWifi)
        val btnActionLock = findViewById<android.view.View>(R.id.btnActionLock)
        val toolsPrimeNameText = findViewById<TextView>(R.id.toolsPrimeNameText)
        val btnPrimeCall = findViewById<ImageButton>(R.id.btnPrimeCall)
        val btnPrimeWhatsApp = findViewById<ImageButton>(R.id.btnPrimeWhatsApp)
        val btnActionYoutube = findViewById<Button>(R.id.btnActionYoutube)
        val btnActionMaps = findViewById<Button>(R.id.btnActionMaps)

        closeBtn?.setOnClickListener { dismiss() }

        // Prime Contact info
        val prefs = context.getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString("prime_contacts_json", null)
        var primeName = "Priya"
        var primeNumber = "+919876543210"

        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                if (array.length() > 0) {
                    val obj = array.getJSONObject(0)
                    primeName = obj.optString("name", "Priya")
                    primeNumber = obj.optString("number", "+919876543210")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        toolsPrimeNameText?.text = "$primeName ($primeNumber)"

        // SOS
        btnTriggerSos?.setOnClickListener {
            viewModel.executeCommand(AppCommand(AppCommand.TYPE_EMERGENCY_SOS))
            onActionTriggered("🚨 Emergency SOS dispatched to $primeName!")
            dismiss()
        }

        // Torch
        var isTorch = false
        btnActionTorch?.setOnClickListener {
            isTorch = !isTorch
            if (isTorch) {
                viewModel.executeCommand(AppCommand(AppCommand.TYPE_FLASHLIGHT_ON))
                torchStateText?.text = "Torch ON 💡"
                onActionTriggered("Torch on kar di gayi hai.")
            } else {
                viewModel.executeCommand(AppCommand(AppCommand.TYPE_FLASHLIGHT_OFF))
                torchStateText?.text = "Torch OFF"
                onActionTriggered("Torch band kar di gayi hai.")
            }
        }

        // Battery
        btnActionBattery?.setOnClickListener {
            viewModel.executeCommand(AppCommand(AppCommand.TYPE_GET_BATTERY))
            dismiss()
        }

        // Wifi
        btnActionWifi?.setOnClickListener {
            viewModel.executeCommand(AppCommand(AppCommand.TYPE_WIFI_ON))
            dismiss()
        }

        // Lock
        btnActionLock?.setOnClickListener {
            viewModel.executeCommand(AppCommand(AppCommand.TYPE_LOCK_DEVICE))
            dismiss()
        }

        // Prime Call
        btnPrimeCall?.setOnClickListener {
            viewModel.executeCommand(AppCommand(AppCommand.TYPE_CALL, mapOf("name" to primeName)))
            dismiss()
        }

        // Prime WhatsApp
        btnPrimeWhatsApp?.setOnClickListener {
            val waUrl = "https://wa.me/${primeNumber.replace(Regex("[^0-9]"), "")}?text=${Uri.encode("Hey $primeName! Message from MYRA.")}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                viewModel.executeCommand(AppCommand(AppCommand.TYPE_OPEN_APP, mapOf("app_name" to "whatsapp")))
            }
            dismiss()
        }

        // YouTube
        btnActionYoutube?.setOnClickListener {
            viewModel.executeCommand(AppCommand(AppCommand.TYPE_OPEN_APP, mapOf("app_name" to "youtube")))
            dismiss()
        }

        // Maps
        btnActionMaps?.setOnClickListener {
            viewModel.executeCommand(AppCommand(AppCommand.TYPE_OPEN_APP, mapOf("app_name" to "maps")))
            dismiss()
        }
    }
}
