package com.myra.assistant.social

enum class SocialPlatform {
    INSTAGRAM,
    FACEBOOK
}

enum class SocialAction {
    POST_FEED,
    POST_STORY,
    POST_REEL,
    POST_TEXT
}

enum class SocialTaskStatus {
    IDLE,
    RUNNING,
    AWAITING_CONFIRMATION,
    USER_ACTION_REQUIRED,
    PUBLISHED,
    SUBMITTED_UNVERIFIED,
    DRAFT_READY,
    FAILED,
    CANCELLED,
    REJECTED
}

data class SocialMediaTask(
    val taskId: String,
    val platform: SocialPlatform,
    val action: SocialAction,
    val mediaUri: String? = null,
    val mediaMimeType: String? = null,
    var caption: String? = null,
    val mode: String = "publish", // "publish" or "draft"
    val targetAccount: String? = null,
    var currentStep: Int = 0,
    var status: SocialTaskStatus = SocialTaskStatus.IDLE,
    var retryCount: Int = 0,
    val maxRetries: Int = 3,
    var failureReason: String? = null,
    var confirmationQuestion: String? = null,
    var isConfirmed: Boolean = false
)

enum class AgentState {
    IDLE,
    OPENING_APP,
    OBSERVING,
    RESOLVING_TARGET,
    VALIDATING_TARGET,
    EXECUTING_ACTION,
    WAITING_FOR_UI,
    VERIFYING,
    NEXT_STEP,
    RETRY,
    REOBSERVE,
    WAITING_FOR_CONFIRMATION,
    WAITING_FOR_USER,
    COMPLETED,
    FAILED,
    CANCELLED
}

object TaskStateMachine {

    private val legalTransitions = mapOf(
        AgentState.IDLE to setOf(AgentState.OPENING_APP, AgentState.OBSERVING, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.OPENING_APP to setOf(AgentState.OBSERVING, AgentState.WAITING_FOR_UI, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.OBSERVING to setOf(AgentState.RESOLVING_TARGET, AgentState.WAITING_FOR_USER, AgentState.WAITING_FOR_CONFIRMATION, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.RESOLVING_TARGET to setOf(AgentState.VALIDATING_TARGET, AgentState.RETRY, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.VALIDATING_TARGET to setOf(AgentState.EXECUTING_ACTION, AgentState.WAITING_FOR_CONFIRMATION, AgentState.RETRY, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.EXECUTING_ACTION to setOf(AgentState.WAITING_FOR_UI, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.WAITING_FOR_UI to setOf(AgentState.REOBSERVE, AgentState.VERIFYING, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.VERIFYING to setOf(AgentState.NEXT_STEP, AgentState.COMPLETED, AgentState.RETRY, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.NEXT_STEP to setOf(AgentState.OBSERVING, AgentState.COMPLETED, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.RETRY to setOf(AgentState.REOBSERVE, AgentState.FAILED),
        AgentState.REOBSERVE to setOf(AgentState.OBSERVING, AgentState.FAILED, AgentState.CANCELLED),
        AgentState.WAITING_FOR_CONFIRMATION to setOf(AgentState.EXECUTING_ACTION, AgentState.CANCELLED, AgentState.FAILED),
        AgentState.WAITING_FOR_USER to setOf(AgentState.REOBSERVE, AgentState.CANCELLED, AgentState.FAILED),
        AgentState.COMPLETED to emptySet(),
        AgentState.FAILED to emptySet(),
        AgentState.CANCELLED to emptySet()
    )

    fun canTransition(from: AgentState, to: AgentState): Boolean {
        if (from == to) return true
        val allowed = legalTransitions[from] ?: return false
        return allowed.contains(to)
    }

    fun transition(currentState: AgentState, nextState: AgentState): AgentState {
        if (!canTransition(currentState, nextState)) {
            throw IllegalStateException("Illegal state transition from $currentState to $nextState")
        }
        return nextState
    }
}
