package com.myra.assistant.social

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.myra.assistant.service.AccessibilityHelperService
import java.util.concurrent.atomic.AtomicLong

class AccessibilityScreenDriver : ScreenDriver {

    private val observationCounter = AtomicLong(0)
    private var latestObservation: ScreenObservation? = null
    private val activeNodeMap = mutableMapOf<String, AccessibilityNodeInfo>()

    override fun observeScreen(): ScreenObservation? {
        val service = AccessibilityHelperService.instance ?: return null
        val root = service.rootInActiveWindow ?: return null

        val obsId = "obs-${observationCounter.incrementAndGet()}"
        val elements = mutableListOf<UiElement>()
        activeNodeMap.clear()

        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        val currentEvStamp = AccessibilityHelperService.eventStamp
        val currentWinStamp = AccessibilityHelperService.windowStamp

        traverseNode(root, obsId, elements, 0, -1, "0")

        val obs = ScreenObservation(
            observationId = obsId,
            packageName = root.packageName?.toString() ?: "",
            windowClass = root.className?.toString() ?: "",
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            eventStamp = currentEvStamp,
            windowStamp = currentWinStamp,
            elements = elements
        )
        latestObservation = obs
        return obs
    }

    private fun traverseNode(
        node: AccessibilityNodeInfo?,
        obsId: String,
        list: MutableList<UiElement>,
        depth: Int,
        parentIdx: Int,
        path: String
    ) {
        if (node == null) return

        val index = list.size
        val elementId = "$obsId#$index"
        val rect = Rect()
        node.getBoundsInScreen(rect)

        val uiBounds = UiBounds(rect.left, rect.top, rect.right, rect.bottom)
        val isPass = node.isPassword

        val el = UiElement(
            elementId = elementId,
            observationId = obsId,
            index = index,
            packageName = node.packageName?.toString() ?: "",
            className = node.className?.toString() ?: "",
            text = if (isPass) "" else (node.text?.toString() ?: ""),
            contentDescription = node.contentDescription?.toString() ?: "",
            hintText = node.hintText?.toString() ?: "",
            resourceId = node.viewIdResourceName ?: "",
            bounds = uiBounds,
            clickable = node.isClickable,
            longClickable = node.isLongClickable,
            enabled = node.isEnabled,
            selected = node.isSelected,
            focused = node.isFocused,
            focusable = node.isFocusable,
            scrollable = node.isScrollable,
            editable = node.isEditable,
            checkable = node.isCheckable,
            checked = node.isChecked,
            isPassword = isPass,
            visible = node.isVisibleToUser,
            depth = depth,
            parentIndex = parentIdx,
            parentPath = path
        )

        list.add(el)
        activeNodeMap[elementId] = node

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, obsId, list, depth + 1, index, "$path/$i")
        }
    }

    override fun clickElement(elementId: String): Boolean {
        if (!isObservationCurrent(elementId.substringBefore('#'))) return false
        val node = activeNodeMap[elementId] ?: return false
        if (!node.refresh()) return false

        if (node.isClickable) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        var parent = node.parent
        while (parent != null) {
            if (parent.isClickable) {
                return parent.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            }
            parent = parent.parent
        }
        return false
    }

    override fun gestureTap(elementId: String): Boolean {
        val service = AccessibilityHelperService.instance ?: return false
        val node = activeNodeMap[elementId] ?: return false
        if (!node.refresh()) return false

        val rect = Rect()
        node.getBoundsInScreen(rect)
        if (rect.isEmpty) return false

        val x = rect.centerX().toFloat()
        val y = rect.centerY().toFloat()

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 50))
            .build()

        return service.dispatchGesture(gesture, null, null)
    }

    override fun typeText(elementId: String, text: String): Boolean {
        val node = activeNodeMap[elementId] ?: return false
        if (!node.refresh()) return false
        if (!node.isEditable) return false

        val args = Bundle().apply {
            putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
    }

    override fun focusElement(elementId: String): Boolean {
        val node = activeNodeMap[elementId] ?: return false
        if (!node.refresh()) return false
        return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    override fun scroll(forward: Boolean): Boolean {
        val service = AccessibilityHelperService.instance ?: return false
        val root = service.rootInActiveWindow ?: return false
        val action = if (forward) AccessibilityNodeInfo.ACTION_SCROLL_FORWARD else AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD
        return root.performAction(action)
    }

    override fun performGlobalAction(action: GlobalNavAction): Boolean {
        val service = AccessibilityHelperService.instance ?: return false
        val globalAction = when (action) {
            GlobalNavAction.BACK -> AccessibilityService.GLOBAL_ACTION_BACK
            GlobalNavAction.HOME -> AccessibilityService.GLOBAL_ACTION_HOME
            GlobalNavAction.RECENTS -> AccessibilityService.GLOBAL_ACTION_RECENTS
        }
        return service.performGlobalAction(globalAction)
    }

    override fun isObservationCurrent(observationId: String): Boolean {
        val latest = latestObservation ?: return false
        return latest.observationId == observationId
    }

    override fun getWindowStamp(): Long {
        return AccessibilityHelperService.windowStamp
    }

    override fun getEventStamp(): Long {
        return AccessibilityHelperService.eventStamp
    }
}
