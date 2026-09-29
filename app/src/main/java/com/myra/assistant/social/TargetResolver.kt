package com.myra.assistant.social

enum class RegionPreference {
    ANY,
    TOP,
    BOTTOM
}

data class TargetSelector(
    val resourceIds: List<String> = emptyList(),
    val exactDescriptions: List<String> = emptyList(),
    val exactTexts: List<String> = emptyList(),
    val normalizedTexts: List<String> = emptyList(),
    val semanticHints: List<String> = emptyList(),
    val className: String? = null,
    val excludeTexts: List<String> = emptyList(),
    val allowedPackages: List<String> = emptyList(),
    val requireClickable: Boolean = true,
    val requireEditable: Boolean = false,
    val regionPreference: RegionPreference = RegionPreference.ANY,
    // Structural Anchor: e.g. "clickable on same row and strictly to the left of anchorDescription"
    val anchorDescription: String? = null,
    val anchorToLeft: Boolean = false
)

object TargetResolver {

    /**
     * Resolves the best matching UiElement from a fresh ScreenObservation.
     * Pure function for unit-testing.
     */
    fun resolve(
        observation: ScreenObservation,
        selector: TargetSelector
    ): UiElement? {
        val candidates = mutableListOf<Pair<UiElement, Int>>() // Pair of (Element, PriorityScore)

        // Find optional anchor if defined
        var anchorElement: UiElement? = null
        if (!selector.anchorDescription.isNullOrBlank()) {
            anchorElement = observation.elements.firstOrNull {
                it.contentDescription.equals(selector.anchorDescription, ignoreCase = true) ||
                        it.text.equals(selector.anchorDescription, ignoreCase = true)
            }
        }

        for (el in observation.elements) {
            if (!passesBasicFilters(el, selector, observation)) continue

            // Check structural anchor condition if specified
            if (anchorElement != null && selector.anchorToLeft) {
                val onSameRow = Math.abs(el.bounds.centerY - anchorElement.bounds.centerY) < 40
                val strictlyLeft = el.bounds.right <= anchorElement.bounds.left
                if (!onSameRow || !strictlyLeft) continue
            }

            val priority = matchPriority(el, selector)
            if (priority > 0) {
                candidates.add(Pair(el, priority))
            }
        }

        if (candidates.isEmpty()) return null

        // Deterministic tie-breaking:
        // 1. Highest priority (lowest number: 1 is best)
        // 2. Clickable
        // 3. Enabled
        // 4. Region preference
        // 5. Smaller area (specific buttons over container views)
        // 6. Top position
        // 7. Left position
        // 8. Element index
        return candidates.sortedWith(
            compareBy<Pair<UiElement, Int>> { it.second } // priority
                .thenByDescending { it.first.clickable }
                .thenByDescending { it.first.enabled }
                .thenBy { regionScore(it.first, selector.regionPreference, observation.screenHeight) }
                .thenBy { it.first.bounds.area }
                .thenBy { it.first.bounds.top }
                .thenBy { it.first.bounds.left }
                .thenBy { it.first.index }
        ).first().first
    }

    private fun passesBasicFilters(
        el: UiElement,
        selector: TargetSelector,
        observation: ScreenObservation
    ): Boolean {
        if (!el.visible) return false
        if (el.bounds.isEmpty) return false

        // Bounds must be within screen dimensions
        if (el.bounds.right < 0 || el.bounds.bottom < 0) return false
        if (el.bounds.left > observation.screenWidth || el.bounds.top > observation.screenHeight) return false

        // Allowed packages
        if (selector.allowedPackages.isNotEmpty() && !selector.allowedPackages.contains(el.packageName)) {
            return false
        }

        // Exclude texts / descriptions
        for (ex in selector.excludeTexts) {
            val exClean = ex.lowercase()
            if (el.text.lowercase().contains(exClean) || el.contentDescription.lowercase().contains(exClean)) {
                return false
            }
        }

        // Require clickable
        if (selector.requireClickable && !el.clickable && !el.editable) {
            return false
        }

        // Require editable
        if (selector.requireEditable && !el.editable) {
            return false
        }

        // Passwords must never be typed into or treated as normal inputs
        if (selector.requireEditable && el.isPassword) {
            return false
        }

        // Class name match
        if (selector.className != null && !el.className.endsWith(selector.className, ignoreCase = true)) {
            return false
        }

        return true
    }

    private fun matchPriority(el: UiElement, selector: TargetSelector): Int {
        val resId = el.resourceId.substringAfterLast('/')
        val text = el.safeText.trim()
        val desc = el.contentDescription.trim()

        // Priority 1: resourceId
        if (selector.resourceIds.any { it.equals(resId, ignoreCase = true) || it.equals(el.resourceId, ignoreCase = true) }) {
            return 1
        }

        // Priority 2: exact contentDescription
        if (desc.isNotEmpty() && selector.exactDescriptions.any { it.equals(desc, ignoreCase = true) }) {
            return 2
        }

        // Priority 3: exact visible text
        if (text.isNotEmpty() && selector.exactTexts.any { it.equals(text, ignoreCase = true) }) {
            return 3
        }

        // Priority 4: normalized text (lowercase, stripped punctuation)
        val normText = normalize(text)
        val normDesc = normalize(desc)
        if (selector.normalizedTexts.any {
                val n = normalize(it)
                (normText.isNotEmpty() && normText == n) || (normDesc.isNotEmpty() && normDesc == n)
            }) {
            return 4
        }

        // Priority 5: semantic hint in SHORT label (max 48 chars to avoid feed caption false matches)
        if (text.length in 1..48 || desc.length in 1..48 || el.hintText.length in 1..48) {
            for (hint in selector.semanticHints) {
                val h = hint.lowercase()
                if (normText.contains(h) || normDesc.contains(h) || el.hintText.lowercase().contains(h)) {
                    return 5
                }
            }
        }

        // Priority 6: Class + structural anchor
        if (selector.anchorDescription != null && selector.className != null && el.className.endsWith(selector.className, ignoreCase = true)) {
            return 6
        }

        return 0 // No match
    }

    private fun normalize(str: String): String {
        return str.lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .trim()
    }

    private fun regionScore(el: UiElement, pref: RegionPreference, screenHeight: Int): Int {
        val mid = screenHeight / 2
        return when (pref) {
            RegionPreference.ANY -> 0
            RegionPreference.TOP -> if (el.centerY <= mid) 0 else 1
            RegionPreference.BOTTOM -> if (el.centerY >= mid) 0 else 1
        }
    }
}
