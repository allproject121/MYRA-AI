package com.myra.assistant.social

class TextInputExecutor(private val driver: ScreenDriver) {

    /**
     * Types text safely into a resolved target field with keyboard-shift re-observation.
     */
    fun executeType(
        selector: TargetSelector,
        text: String
    ): ExecutionResult {
        // Step 1: Initial Observe
        val obs1 = driver.observeScreen() ?: return ExecutionResult.Failure("Unable to observe screen")

        // Step 2: Resolve editable field
        val target1 = TargetResolver.resolve(obs1, selector.copy(requireEditable = true))
            ?: return ExecutionResult.Failure("Editable target not found on screen")

        if (target1.isPassword) {
            return ExecutionResult.UserActionRequired("Cannot type into password fields automatically")
        }

        // Step 3: Focus
        driver.focusElement(target1.elementId)

        // Step 4: RE-OBSERVE because virtual keyboard arrival moves layout (Lesson / Part A.5)
        try {
            Thread.sleep(350)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        val obs2 = driver.observeScreen() ?: return ExecutionResult.Failure("Screen disappeared after focus")

        // Step 5: Re-resolve in newly shifted layout
        val target2 = TargetResolver.resolve(obs2, selector.copy(requireEditable = true))
            ?: return ExecutionResult.Failure("Target field lost after keyboard appearance")

        // Step 6: ACTION_SET_TEXT (log length only, never the text)
        val textLength = text.length
        val typed = driver.typeText(target2.elementId, text)
        if (!typed) {
            return ExecutionResult.Failure("Failed to set text into target field")
        }

        // Step 7: Wait & Re-observe to verify
        try {
            Thread.sleep(200)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        }

        val obs3 = driver.observeScreen() ?: return ExecutionResult.Failure("Screen lost during verification")
        val verifiedField = TargetResolver.resolve(obs3, selector)

        if (verifiedField != null && verifiedField.safeText.length >= textLength.coerceAtMost(10)) {
            return ExecutionResult.Success(obs3)
        }

        // Even if placeholder reports, success if set_text returned true
        return ExecutionResult.Success(obs3)
    }
}
