package com.myra.assistant.social

import java.security.MessageDigest

/**
 * Model of an accessibility node on screen.
 * Plain Kotlin data structure — no android.graphics.Rect or android.net.Uri in core code
 * to enable pure JVM unit testing.
 */
data class UiBounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2
    val area: Int get() = maxOf(0, width) * maxOf(0, height)
    val isEmpty: Boolean get() = width <= 0 || height <= 0
}

data class UiElement(
    val elementId: String,            // Embeds observationId e.g. "obs-12#5"
    val observationId: String,
    val index: Int,
    val packageName: String,
    val className: String,
    val text: String = "",
    val contentDescription: String = "",
    val hintText: String = "",
    val resourceId: String = "",
    val bounds: UiBounds,
    val clickable: Boolean = false,
    val longClickable: Boolean = false,
    val enabled: Boolean = true,
    val selected: Boolean = false,
    val focused: Boolean = false,
    val focusable: Boolean = false,
    val scrollable: Boolean = false,
    val editable: Boolean = false,
    val checkable: Boolean = false,
    val checked: Boolean = false,
    val isPassword: Boolean = false,
    val visible: Boolean = true,
    val depth: Int = 0,
    val parentIndex: Int = -1,
    val parentPath: String = ""
) {
    val centerX: Int get() = bounds.centerX
    val centerY: Int get() = bounds.centerY

    /**
     * Sanitized text representation — passwords redacted at capture time.
     */
    val safeText: String
        get() = if (isPassword) "[REDACTED_PASSWORD]" else text
}

data class ScreenObservation(
    val observationId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val packageName: String,
    val windowClass: String,
    val screenWidth: Int,
    val screenHeight: Int,
    val eventStamp: Long,
    val windowStamp: Long,
    val elements: List<UiElement>
) {
    /**
     * Computes a deterministic structural signature of the screen.
     */
    val structuralSignature: String by lazy {
        val raw = StringBuilder()
        raw.append("$packageName|$windowClass|w$screenWidth|h$screenHeight|")
        elements.forEach { el ->
            raw.append("${el.resourceId}:${el.safeText}:${el.contentDescription}:${el.bounds.left},${el.bounds.top},${el.bounds.right},${el.bounds.bottom}:${el.enabled}:${el.selected}:${el.clickable};")
        }
        val digest = MessageDigest.getInstance("MD5").digest(raw.toString().toByteArray())
        digest.joinToString("") { "%02x".format(it) }
    }

    /**
     * Deterministic numbered list of visible interactive elements for debugging.
     */
    fun toDebugString(): String {
        val sb = StringBuilder()
        sb.append("Observation [$observationId] pkg=$packageName win=$windowClass evStamp=$eventStamp winStamp=$windowStamp count=${elements.size}\n")
        elements.filter { it.visible && (it.clickable || it.editable || it.text.isNotBlank() || it.contentDescription.isNotBlank()) }
            .forEachIndexed { idx, el ->
                sb.append("ELEMENT $idx: [${el.elementId}] id=${el.resourceId.substringAfterLast('/')} text=\"${el.safeText}\" desc=\"${el.contentDescription}\" bounds=(${el.bounds.left},${el.bounds.top},${el.bounds.right},${el.bounds.bottom}) clickable=${el.clickable} enabled=${el.enabled} editable=${el.editable}\n")
            }
        return sb.toString()
    }
}
