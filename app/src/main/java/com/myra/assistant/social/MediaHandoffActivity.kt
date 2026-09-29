package com.myra.assistant.social

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import java.io.FileOutputStream

class MediaHandoffActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val intent = intent
        val action = intent.action
        val type = intent.type

        if (Intent.ACTION_SEND == action && type != null) {
            val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            if (uri != null && (type.startsWith("image/") || type.startsWith("video/"))) {
                showConfirmationDialog(uri, type)
                return
            }
        }

        Toast.makeText(this, "Unsupported media handoff", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showConfirmationDialog(uri: Uri, mimeType: String) {
        AlertDialog.Builder(this)
            .setTitle("MYRA Social Media Agent")
            .setMessage("Give this ${if (mimeType.startsWith("video/")) "video" else "photo"} to MYRA for posting to Instagram or Facebook?")
            .setPositiveButton("Yes, hand over") { _, _ ->
                copyAndSaveMedia(uri, mimeType)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "Handoff cancelled", Toast.LENGTH_SHORT).show()
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun copyAndSaveMedia(uri: Uri, mimeType: String) {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val ext = if (mimeType.startsWith("video/")) ".mp4" else ".jpg"
            val file = File(cacheDir, "myra_social_media_${System.currentTimeMillis()}$ext")
            val outputStream = FileOutputStream(file)

            inputStream?.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }

            val resolver = MediaResolver(this)
            resolver.addMediaToInbox(uri.toString(), mimeType, file.absolutePath)

            Toast.makeText(this, "Media handed over to MYRA! Ready to post.", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to copy media: ${e.message}", Toast.LENGTH_SHORT).show()
        }
        finish()
    }
}
