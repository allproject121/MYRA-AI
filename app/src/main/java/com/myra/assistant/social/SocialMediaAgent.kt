package com.myra.assistant.social

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SocialMediaAgent(
    private val context: Context,
    private val driver: ScreenDriver,
    private val mediaResolver: MediaResolver
) {

    private val actionExecutor = UiActionExecutor(driver)
    private val textExecutor = TextInputExecutor(driver)

    var currentTask: SocialMediaTask? = null
        private set

    /**
     * Executes or starts a social media task.
     * Coroutine running on Dispatchers.IO.
     */
    suspend fun executeTask(task: SocialMediaTask): SocialMediaTask = withContext(Dispatchers.IO) {
        currentTask = task
        task.status = SocialTaskStatus.RUNNING

        // Verify target app is installed
        val targetPkg = when (task.platform) {
            SocialPlatform.INSTAGRAM -> InstagramSelectors.PACKAGE_INSTAGRAM
            SocialPlatform.FACEBOOK -> FacebookSelectors.PACKAGE_FACEBOOK
        }

        if (!isAppInstalled(targetPkg)) {
            task.status = SocialTaskStatus.FAILED
            task.failureReason = "App $targetPkg is not installed on this device"
            return@withContext task
        }

        // Verify media if required
        var resolvedMedia: SharedMediaItem? = null
        if (task.action != SocialAction.POST_TEXT) {
            resolvedMedia = mediaResolver.resolveMedia(task.mediaUri)
            if (resolvedMedia == null) {
                task.status = SocialTaskStatus.FAILED
                task.failureReason = "media_required: Please share the photo or video from gallery to MYRA first"
                return@withContext task
            }
        }

        try {
            when (task.platform) {
                SocialPlatform.INSTAGRAM -> runInstagramFlow(task, resolvedMedia)
                SocialPlatform.FACEBOOK -> runFacebookFlow(task, resolvedMedia)
            }
        } catch (e: Exception) {
            task.status = SocialTaskStatus.FAILED
            task.failureReason = e.message ?: "Task execution failed"
        }

        return@withContext task
    }

    /**
     * Resumes or controls a task waiting at the Confirmation Gate.
     */
    suspend fun handleControl(command: String, taskId: String? = null): SocialMediaTask = withContext(Dispatchers.IO) {
        val task = currentTask ?: return@withContext SocialMediaTask(
            taskId = taskId ?: "none",
            platform = SocialPlatform.INSTAGRAM,
            action = SocialAction.POST_FEED,
            status = SocialTaskStatus.FAILED,
            failureReason = "No active task in progress"
        )

        when (command.lowercase().trim()) {
            "confirm", "yes", "haan", "share" -> {
                if (task.status == SocialTaskStatus.AWAITING_CONFIRMATION) {
                    task.isConfirmed = true
                    task.status = SocialTaskStatus.RUNNING
                    // Resume to final publish step
                    when (task.platform) {
                        SocialPlatform.INSTAGRAM -> finishInstagramPublish(task)
                        SocialPlatform.FACEBOOK -> finishFacebookPublish(task)
                    }
                }
            }
            "reject", "cancel", "no", "nahi", "mat karo" -> {
                task.status = SocialTaskStatus.REJECTED
                task.failureReason = "User rejected publication"
            }
            "resume" -> {
                // Resume after user completed 2FA/login
                task.status = SocialTaskStatus.RUNNING
                executeTask(task)
            }
            else -> {
                // Status query
            }
        }

        return@withContext task
    }

    // ==========================================
    // INSTAGRAM FLOW (Lessons 2, 3, 4, 5, 7, 8)
    // ==========================================
    private fun runInstagramFlow(task: SocialMediaTask, media: SharedMediaItem?) {
        val initialWindowStamp = driver.getWindowStamp()

        // 1. Share media to explicit component (Lesson 6)
        val shareComponent = when (task.action) {
            SocialAction.POST_STORY -> InstagramSelectors.SHARE_COMPONENTS_STORY.firstOrNull()
            SocialAction.POST_REEL -> InstagramSelectors.SHARE_COMPONENTS_REEL.firstOrNull()
            else -> InstagramSelectors.SHARE_COMPONENTS_FEED.firstOrNull()
        }

        if (media != null && shareComponent != null) {
            launchExplicitShareIntent(
                packageName = InstagramSelectors.PACKAGE_INSTAGRAM,
                componentClassName = shareComponent,
                uriString = media.uriString,
                mimeType = media.mimeType
            )
        } else {
            launchApp(InstagramSelectors.PACKAGE_INSTAGRAM)
        }

        // 2. Wait for NEW window after intent (Lesson 2: Prevents running on stale caption screen)
        waitForNewWindow(initialWindowStamp, InstagramSelectors.PACKAGE_INSTAGRAM)

        // 3. Handle Flow Steps
        if (task.action == SocialAction.POST_STORY) {
            handleInstagramStoryFlow(task)
        } else {
            handleInstagramFeedOrReelFlow(task)
        }
    }

    private fun handleInstagramFeedOrReelFlow(task: SocialMediaTask) {
        // Step A: Tap NEXT until Caption screen or Share button is visible (Lesson 3: Wait for button OR destination)
        var maxNextAttempts = 3
        while (maxNextAttempts-- > 0) {
            val obs = driver.observeScreen() ?: break

            // Check if already reached caption/share screen (destination arrived early)
            val captionField = TargetResolver.resolve(obs, InstagramSelectors.CAPTION_INPUT)
            val publishBtn = TargetResolver.resolve(obs, InstagramSelectors.PUBLISH_FEED_OR_REEL)
            if (captionField != null || publishBtn != null) {
                break
            }

            // Click Next
            val res = actionExecutor.executeClick(InstagramSelectors.NEXT_BUTTON)
            if (res is ExecutionResult.UserActionRequired) {
                task.status = SocialTaskStatus.USER_ACTION_REQUIRED
                task.failureReason = res.reason
                return
            }
            if (res !is ExecutionResult.Success) {
                break
            }
        }

        // Step B: Type Caption if provided
        if (!task.caption.isNullOrBlank()) {
            textExecutor.executeType(InstagramSelectors.CAPTION_INPUT, task.caption!!)
        }

        // Step C: CONFIRMATION GATE (Part A / 9)
        val freshObs = driver.observeScreen()
        val publishControl = freshObs?.let { TargetResolver.resolve(it, InstagramSelectors.PUBLISH_FEED_OR_REEL) }

        if (publishControl != null && publishControl.enabled) {
            if (task.mode == "draft") {
                task.status = SocialTaskStatus.DRAFT_READY
                return
            }

            task.status = SocialTaskStatus.AWAITING_CONFIRMATION
            task.confirmationQuestion = "Instagram post is ready with caption \"${task.caption ?: ""}\". Should I share it now?"
            return
        }

        task.status = SocialTaskStatus.FAILED
        task.failureReason = "Publish button not found on final screen"
    }

    private fun finishInstagramPublish(task: SocialMediaTask) {
        if (!task.isConfirmed) {
            task.status = SocialTaskStatus.FAILED
            task.failureReason = "Safety Gate: Publish refused without confirmation"
            return
        }

        // Publish tap
        val res = actionExecutor.executeClick(InstagramSelectors.PUBLISH_FEED_OR_REEL)
        if (res is ExecutionResult.Success) {
            // Verify success indicator
            val verifyObs = driver.observeScreen()
            val success = verifyObs?.let { TargetResolver.resolve(it, InstagramSelectors.SUCCESS_INDICATORS) }
            if (success != null) {
                task.status = SocialTaskStatus.PUBLISHED
            } else {
                task.status = SocialTaskStatus.SUBMITTED_UNVERIFIED
            }
        } else {
            task.status = SocialTaskStatus.FAILED
            task.failureReason = "Failed to tap Share button"
        }
    }

    private fun handleInstagramStoryFlow(task: SocialMediaTask) {
        // Confirmation Gate for Story
        val freshObs = driver.observeScreen()
        val storyBtn = freshObs?.let { TargetResolver.resolve(it, InstagramSelectors.PUBLISH_STORY) }

        if (storyBtn != null && storyBtn.enabled) {
            if (task.mode == "draft") {
                task.status = SocialTaskStatus.DRAFT_READY
                return
            }
            task.status = SocialTaskStatus.AWAITING_CONFIRMATION
            task.confirmationQuestion = "Instagram Story is ready. Should I share to Your Stories now?"
            return
        }

        task.status = SocialTaskStatus.FAILED
        task.failureReason = "Story publish button not found"
    }

    // ==========================================
    // FACEBOOK FLOW
    // ==========================================
    private fun runFacebookFlow(task: SocialMediaTask, media: SharedMediaItem?) {
        val initialWindowStamp = driver.getWindowStamp()

        if (task.action == SocialAction.POST_TEXT) {
            launchApp(FacebookSelectors.PACKAGE_FACEBOOK)
            waitForNewWindow(initialWindowStamp, FacebookSelectors.PACKAGE_FACEBOOK)

            // 1. Open Composer
            actionExecutor.executeClick(FacebookSelectors.COMPOSER_ENTRY)

            // 2. Type Post Text
            if (!task.caption.isNullOrBlank()) {
                textExecutor.executeType(FacebookSelectors.COMPOSER_INPUT, task.caption!!)
            }

            // 3. Tap Next if present
            actionExecutor.executeClick(FacebookSelectors.NEXT_BUTTON)

            // 4. Confirmation Gate
            val freshObs = driver.observeScreen()
            val publishBtn = freshObs?.let { TargetResolver.resolve(it, FacebookSelectors.PUBLISH_BUTTON) }

            if (publishBtn != null && publishBtn.enabled) {
                if (task.mode == "draft") {
                    task.status = SocialTaskStatus.DRAFT_READY
                    return
                }
                task.status = SocialTaskStatus.AWAITING_CONFIRMATION
                task.confirmationQuestion = "Facebook post is ready: \"${task.caption ?: ""}\". Should I post it?"
                return
            }

            task.status = SocialTaskStatus.FAILED
            task.failureReason = "Facebook publish button not found"
        } else {
            // Media Post
            val shareComponent = FacebookSelectors.SHARE_COMPONENTS_FEED.firstOrNull()
            if (media != null && shareComponent != null) {
                launchExplicitShareIntent(
                    packageName = FacebookSelectors.PACKAGE_FACEBOOK,
                    componentClassName = shareComponent,
                    uriString = media.uriString,
                    mimeType = media.mimeType
                )
            } else {
                launchApp(FacebookSelectors.PACKAGE_FACEBOOK)
            }
            waitForNewWindow(initialWindowStamp, FacebookSelectors.PACKAGE_FACEBOOK)

            if (!task.caption.isNullOrBlank()) {
                textExecutor.executeType(FacebookSelectors.COMPOSER_INPUT, task.caption!!)
            }

            task.status = SocialTaskStatus.AWAITING_CONFIRMATION
            task.confirmationQuestion = "Facebook photo post is ready. Should I publish it?"
        }
    }

    private fun finishFacebookPublish(task: SocialMediaTask) {
        if (!task.isConfirmed) {
            task.status = SocialTaskStatus.FAILED
            task.failureReason = "Safety Gate: Publish refused without confirmation"
            return
        }

        val res = actionExecutor.executeClick(FacebookSelectors.PUBLISH_BUTTON)
        if (res is ExecutionResult.Success) {
            task.status = SocialTaskStatus.PUBLISHED
        } else {
            task.status = SocialTaskStatus.SUBMITTED_UNVERIFIED
        }
    }

    // ==========================================
    // INTENTS & SYSTEM UTILITIES
    // ==========================================
    private fun launchExplicitShareIntent(
        packageName: String,
        componentClassName: String,
        uriString: String,
        mimeType: String
    ) {
        try {
            val uri = Uri.parse(uriString)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                component = ComponentName(packageName, componentClassName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback: general app launch
            launchApp(packageName)
        }
    }

    private fun launchApp(packageName: String) {
        try {
            val intent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun waitForNewWindow(initialStamp: Long, expectedPackage: String, maxWaitMs: Long = 4000L) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < maxWaitMs) {
            val currentStamp = driver.getWindowStamp()
            val obs = driver.observeScreen()
            if (currentStamp > initialStamp && obs?.packageName == expectedPackage) {
                return
            }
            try {
                Thread.sleep(200)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            }
        }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
}
