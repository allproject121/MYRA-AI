package com.myra.assistant.social

interface ScreenDriver {
    /**
     * Reads the current screen and returns a fresh ScreenObservation.
     */
    fun observeScreen(): ScreenObservation?

    /**
     * Clicks the specified element by ID.
     * ID must belong to the latest current observation.
     */
    fun clickElement(elementId: String): Boolean

    /**
     * Taps the re-validated current bounds of the element.
     */
    fun gestureTap(elementId: String): Boolean

    /**
     * Types text into a validated editable element.
     */
    fun typeText(elementId: String, text: String): Boolean

    /**
     * Focuses the element.
     */
    fun focusElement(elementId: String): Boolean

    /**
     * Performs a scroll forward / backward on the screen.
     */
    fun scroll(forward: Boolean): Boolean

    /**
     * Navigates back or home.
     */
    fun performGlobalAction(action: GlobalNavAction): Boolean

    /**
     * Checks if the observation is still fresh and current.
     */
    fun isObservationCurrent(observationId: String): Boolean

    /**
     * Gets the latest window-transition counter.
     */
    fun getWindowStamp(): Long

    /**
     * Gets the latest UI event counter.
     */
    fun getEventStamp(): Long
}

enum class GlobalNavAction {
    BACK,
    HOME,
    RECENTS
}
