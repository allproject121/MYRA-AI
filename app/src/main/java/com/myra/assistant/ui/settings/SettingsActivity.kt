package com.myra.assistant.ui.settings

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.myra.assistant.R
import com.myra.assistant.service.AccessibilityHelperService
import org.json.JSONArray
import org.json.JSONObject

class SettingsActivity : AppCompatActivity() {

    private lateinit var apiKeyInput: EditText
    private lateinit var userNameInput: EditText
    private lateinit var modelSpinner: Spinner
    private lateinit var voiceSpinner: Spinner
    private lateinit var personalityRadioGroup: RadioGroup
    private lateinit var radioGf: RadioButton
    private lateinit var radioProfessional: RadioButton
    private lateinit var radioAssistant: RadioButton
    private lateinit var primeContactsRecycler: RecyclerView
    private lateinit var accessibilityStatusText: TextView
    private lateinit var addPrimeContactBtn: Button
    private lateinit var saveSettingsBtn: Button
    private lateinit var settingsBackBtn: ImageButton

    private val modelOptions = listOf(
        Pair("Native Audio (Human Voice) — DEFAULT", "models/gemini-2.5-flash-native-audio-preview-12-2025"),
        Pair("Flash Live (Fast)", "models/gemini-2.0-flash-live-001"),
        Pair("Pro Audio Dialog", "models/gemini-2.5-flash-preview-native-audio-dialog")
    )

    private val voiceOptions = listOf(
        Pair("Aoede (Female) — Default", "Aoede"),
        Pair("Charon (Male)", "Charon"),
        Pair("Kore (Female)", "Kore"),
        Pair("Fenrir (Male)", "Fenrir"),
        Pair("Puck (Male)", "Puck"),
        Pair("Leda (Female)", "Leda"),
        Pair("Orus (Male)", "Orus"),
        Pair("Zephyr (Female)", "Zephyr")
    )

    private val primeContactsList = mutableListOf<Pair<String, String>>()
    private lateinit var primeAdapter: PrimeContactAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initViews()
        loadPreferences()
        setupListeners()
    }

    private fun initViews() {
        apiKeyInput = findViewById(R.id.apiKeyInput)
        userNameInput = findViewById(R.id.userNameInput)
        modelSpinner = findViewById(R.id.modelSpinner)
        voiceSpinner = findViewById(R.id.voiceSpinner)
        personalityRadioGroup = findViewById(R.id.personalityRadioGroup)
        radioGf = findViewById(R.id.radioGf)
        radioProfessional = findViewById(R.id.radioProfessional)
        radioAssistant = findViewById(R.id.radioAssistant)
        primeContactsRecycler = findViewById(R.id.primeContactsRecycler)
        accessibilityStatusText = findViewById(R.id.accessibilityStatusText)
        addPrimeContactBtn = findViewById(R.id.addPrimeContactBtn)
        saveSettingsBtn = findViewById(R.id.saveSettingsBtn)
        settingsBackBtn = findViewById(R.id.settingsBackBtn)

        // Spinners
        val modelAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, modelOptions.map { it.first })
        modelSpinner.adapter = modelAdapter

        val voiceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, voiceOptions.map { it.first })
        voiceSpinner.adapter = voiceAdapter

        // Prime Contacts Recycler
        primeAdapter = PrimeContactAdapter(primeContactsList) { position ->
            primeContactsList.removeAt(position)
            primeAdapter.notifyItemRemoved(position)
        }
        primeContactsRecycler.layoutManager = LinearLayoutManager(this)
        primeContactsRecycler.adapter = primeAdapter
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        apiKeyInput.setText(prefs.getString("api_key", ""))
        userNameInput.setText(prefs.getString("user_name", "Boss"))

        val savedModel = prefs.getString("gemini_model", modelOptions[0].second)
        val modelIndex = modelOptions.indexOfFirst { it.second == savedModel }.coerceAtLeast(0)
        modelSpinner.setSelection(modelIndex)

        val savedVoice = prefs.getString("gemini_voice", voiceOptions[0].second)
        val voiceIndex = voiceOptions.indexOfFirst { it.second == savedVoice }.coerceAtLeast(0)
        voiceSpinner.setSelection(voiceIndex)

        when (prefs.getString("personality_mode", "GF")) {
            "Professional" -> radioProfessional.isChecked = true
            "Assistant" -> radioAssistant.isChecked = true
            else -> radioGf.isChecked = true
        }

        // Prime Contacts
        val jsonStr = prefs.getString("prime_contacts_json", null)
        primeContactsList.clear()
        if (!jsonStr.isNullOrEmpty()) {
            try {
                val array = JSONArray(jsonStr)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    primeContactsList.add(Pair(obj.getString("name"), obj.getString("number")))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        primeAdapter.notifyDataSetChanged()

        updateAccessibilityStatus()
    }

    private fun updateAccessibilityStatus() {
        val isEnabled = AccessibilityHelperService.isEnabled(this)
        if (isEnabled) {
            accessibilityStatusText.text = "Accessibility Service: ✅ Active"
            accessibilityStatusText.setTextColor(getColor(R.color.status_green))
        } else {
            accessibilityStatusText.text = "Accessibility Service: ❌ Inactive (Tap to Enable)"
            accessibilityStatusText.setTextColor(getColor(R.color.primary_red))
        }
    }

    private fun setupListeners() {
        settingsBackBtn.setOnClickListener { finish() }

        accessibilityStatusText.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        addPrimeContactBtn.setOnClickListener {
            showAddPrimeContactDialog()
        }

        saveSettingsBtn.setOnClickListener {
            savePreferences()
        }
    }

    private fun showAddPrimeContactDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_prime_contact, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.dialogNameInput)
        val numberInput = dialogView.findViewById<EditText>(R.id.dialogNumberInput)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("ADD") { _, _ ->
                val name = nameInput.text.toString().trim()
                val number = numberInput.text.toString().trim()
                if (name.isNotEmpty() && number.isNotEmpty()) {
                    primeContactsList.add(Pair(name, number))
                    primeAdapter.notifyItemInserted(primeContactsList.size - 1)
                }
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    private fun savePreferences() {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        editor.putString("api_key", apiKeyInput.text.toString().trim())
        editor.putString("user_name", userNameInput.text.toString().trim())

        val selectedModel = modelOptions[modelSpinner.selectedItemPosition].second
        editor.putString("gemini_model", selectedModel)

        val selectedVoice = voiceOptions[voiceSpinner.selectedItemPosition].second
        editor.putString("gemini_voice", selectedVoice)

        val personality = when {
            radioProfessional.isChecked -> "Professional"
            radioAssistant.isChecked -> "Assistant"
            else -> "GF"
        }
        editor.putString("personality_mode", personality)

        // Save Prime Contacts JSON
        val array = JSONArray()
        for (contact in primeContactsList) {
            val obj = JSONObject().apply {
                put("name", contact.first)
                put("number", contact.second)
            }
            array.put(obj)
        }
        editor.putString("prime_contacts_json", array.toString())

        editor.apply()
        Toast.makeText(this, "Settings saved! Restart app to apply changes.", Toast.LENGTH_SHORT).show()
        finish()
    }

    override fun onResume() {
        super.onResume()
        updateAccessibilityStatus()
    }

    class PrimeContactAdapter(
        private val contacts: List<Pair<String, String>>,
        private val onDeleteClick: (Int) -> Unit
    ) : RecyclerView.Adapter<PrimeContactAdapter.ViewHolder>() {

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val name: TextView = itemView.findViewById(R.id.primeItemName)
            val number: TextView = itemView.findViewById(R.id.primeItemNumber)
            val deleteBtn: ImageButton = itemView.findViewById(R.id.primeItemDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_prime_contact, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = contacts[position]
            holder.name.text = item.first
            holder.number.text = item.second
            holder.deleteBtn.setOnClickListener { onDeleteClick(position) }
        }

        override fun getItemCount(): Int = contacts.size
    }
}
