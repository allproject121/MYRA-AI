package com.myra.assistant.social

object InstagramSelectors {

    const val PACKAGE_INSTAGRAM = "com.instagram.android"

    // Explicit Component Class Names for Share Targets (Lesson 6)
    val SHARE_COMPONENTS_FEED = listOf(
        "com.instagram.share.handler.ShareHandlerActivity",
        "com.instagram.android.activity.ShareHandlerActivity"
    )

    val SHARE_COMPONENTS_STORY = listOf(
        "com.instagram.share.handler.StoryShareHandlerActivity",
        "com.instagram.android.activity.StoryShareHandlerActivity"
    )

    val SHARE_COMPONENTS_REEL = listOf(
        "com.instagram.share.handler.ReelShareHandlerActivity",
        "com.instagram.android.activity.ReelShareHandlerActivity"
    )

    // Dangerous keywords in component names that MUST be rejected
    val DANGEROUS_TARGET_KEYWORDS = listOf(
        "direct", "group", "profile", "message", "chat", "inbox", "messenger"
    )

    // Structural Anchor for Instagram Home "+" Create button (Lesson 5)
    val CREATE_BUTTON = TargetSelector(
        className = "ImageView",
        anchorDescription = "Instagram Home Feed",
        anchorToLeft = true,
        allowedPackages = listOf(PACKAGE_INSTAGRAM),
        requireClickable = true
    )

    // Next Buttons across Gallery, Editor, and Reels (Lessons 3 & 7)
    val NEXT_BUTTON = TargetSelector(
        resourceIds = listOf(
            "next_button_textview",
            "media_thumbnail_tray_button",
            "clips_right_action_button",
            "action_bar_button_action"
        ),
        exactDescriptions = listOf("Next", "Continue"),
        exactTexts = listOf("Next", "Aage"),
        allowedPackages = listOf(PACKAGE_INSTAGRAM),
        requireClickable = true
    )

    // Caption Input Field (Lesson 7)
    val CAPTION_INPUT = TargetSelector(
        resourceIds = listOf("caption_input_text_view", "caption_text_view"),
        exactDescriptions = listOf("Write a caption...", "Add a caption..."),
        semanticHints = listOf("write a caption", "add a caption", "caption"),
        className = "AutoCompleteTextView",
        allowedPackages = listOf(PACKAGE_INSTAGRAM),
        requireClickable = true,
        requireEditable = true
    )

    // Share / Publish Button for Feed & Reel (Lessons 7 & 8)
    val PUBLISH_FEED_OR_REEL = TargetSelector(
        resourceIds = listOf("share_footer_button", "share_button"),
        exactDescriptions = listOf("Share", "Post"),
        exactTexts = listOf("Share", "Post"),
        excludeTexts = listOf("Save draft", "Draft", "Subscribers"),
        allowedPackages = listOf(PACKAGE_INSTAGRAM),
        requireClickable = true,
        regionPreference = RegionPreference.BOTTOM
    )

    // Story Publish Button (Lesson 7: Plural "Your stories")
    val PUBLISH_STORY = TargetSelector(
        resourceIds = listOf("your_story_share_shortcut_button"),
        exactDescriptions = listOf("Your stories", "Your story"),
        exactTexts = listOf("Your stories", "Your story"),
        excludeTexts = listOf("Close Friends", "Subscribers"),
        allowedPackages = listOf(PACKAGE_INSTAGRAM),
        requireClickable = true,
        regionPreference = RegionPreference.BOTTOM
    )

    // Success Indicators
    val SUCCESS_INDICATORS = TargetSelector(
        exactTexts = listOf("Finishing up...", "Posted", "Shared", "Your reel has been shared"),
        exactDescriptions = listOf("Post uploaded", "Story uploaded"),
        requireClickable = false
    )
}
