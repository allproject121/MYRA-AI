package com.myra.assistant.social

sealed class ExecutionResult {
    data class Success(val observation: ScreenObservation) : ExecutionResult()
    data class UserActionRequired(val reason: String) : ExecutionResult()
    data class StaleTarget(val reason: String) : ExecutionResult()
    data class Failure(val reason: String) : ExecutionResult()
}

class UiActionExecutor(
    private val driver: ScreenDriver,
    private val minSettleMs: Long = 400L,
    private val maxSettleMs: Long = 3000L
) {

    /**
     * Checks if the screen contains login, CAPTCHA, 2FA, or runtime permissions.
     */
    fun checkSecurityGuard(observation: ScreenObservation): String? {
        // 1. Runtime permission controller
        if (observation.packageName.contains("permissioncontroller")) {
            return "Runtime permission prompt detected. Please grant or deny on screen."
        }

        // 2. Passwords / Security checkpoints
        val hasPasswordField = observation.elements.any { it.isPassword }
        if (hasPasswordField) {
            return "Security checkpoint / login password screen detected. Please log in manually."
        }

        // 3. CAPTCHA / 2FA / Verification code
        val containsSecurityText = observation.elements.any {
            val t = it.safeText.lowercase()
            val d = it.contentDescription.lowercase()
            t.contains("captcha") || d.contains("captcha") ||
                    t.contains("two-factor") || d.contains("two-factor") ||
                    t.contains("security code") || t.contains("verification code")
        }
        if (containsSecurityText) {
            return "Security challenge / 2FA code required. Please complete on device."
        }

        return null
    }

    /**
     * Closed-loop click execution:
     * OBSERVE -> RESOLVE -> VALIDATE -> FRESHNESS CHECK -> CLICK -> WAIT FOR UI -> RE-OBSERVE
     */
    fun executeClick(
        selector: TargetSelector,
        expectedNewWindow: Boolean = false,
        initialWindowStamp: Long = 0L
    ): ExecutionResult {
        // Step 1: Observe
        val obs = driver.observeScreen() ?: return ExecutionResult.Failure("Unable to read screen")

        // Step 2: Guard Check
        val guardIssue = checkSecurityGuard(obs)
        if (guardIssue != null) {
            return ExecutionResult.UserActionRequired(guardIssue)
        }

        // Step 3: Resolve
        val target = TargetResolver.resolve(obs, selector)
            ?: return ExecutionResult.Failure("Target element not found on current screen")

        // Step 4: Validate
        if (!target.enabled) {
            return ExecutionResult.Failure("Target element is disabled")
        }

        // Step 5: Freshness Check
        if (!driver.isObservationCurrent(obs.observationId)) {
            return ExecutionResult.StaleTarget("Screen changed before click could be performed")
        }

        // Step 6: Click
        val clicked = driver.clickElement(target.elementId) || driver.gestureTap(target.elementId)
        if (!clicked) {
            return ExecutionResult.Failure("Driver failed to perform click action")
        }

        // Step 7: Wait for UI change
        try {
            Thread.sleep(minSettleMs)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        // Step 8: Re-observe
        val newObs = driver.observeScreen() ?: return ExecutionResult.Failure("Screen disappeared after click")

        if (expectedNewWindow && initialWindowStamp > 0) {
            if (driver.getWindowStamp() <= initialWindowStamp) {
                // Window has not transitioned yet
                return ExecutionResult.Failure("Waiting for new window transition...")
            }
        }

        return ExecutionResult.Success(newObs)
    }
}
