package com.myra.assistant.social

object FacebookSelectors {

    const val PACKAGE_FACEBOOK = "com.facebook.katana"
    const val PACKAGE_FACEBOOK_LITE = "com.facebook.lite"

    // Explicit Component Class Names for Share Targets (Lesson 6)
    val SHARE_COMPONENTS_FEED = listOf(
        "com.facebook.composer.shareintent.ImplicitShareIntentHandlerDefaultAlias",
        "com.facebook.composer.shareintent.ImplicitShareIntentHandler"
    )

    val SHARE_COMPONENTS_STORY = listOf(
        "com.facebook.inspiration.shortcut.InpirationCameraShareShortcutActivity"
    )

    val DANGEROUS_TARGET_KEYWORDS = listOf(
        "direct", "group", "profile", "message", "chat", "inbox", "messenger"
    )

    // Composer Entry
    val COMPOSER_ENTRY = TargetSelector(
        exactTexts = listOf("What's on your mind?", "Kuch likhiye..."),
        exactDescriptions = listOf("What's on your mind?", "Make a post on Facebook"),
        allowedPackages = listOf(PACKAGE_FACEBOOK, PACKAGE_FACEBOOK_LITE),
        requireClickable = true
    )

    // Text Input in Composer
    val COMPOSER_INPUT = TargetSelector(
        exactDescriptions = listOf("What's on your mind?", "Post text"),
        semanticHints = listOf("what's on your mind", "say something", "write something"),
        allowedPackages = listOf(PACKAGE_FACEBOOK, PACKAGE_FACEBOOK_LITE),
        requireClickable = true,
        requireEditable = true
    )

    // Next Button (enabled only after text is typed)
    val NEXT_BUTTON = TargetSelector(
        exactTexts = listOf("Next", "Aage"),
        exactDescriptions = listOf("Next"),
        allowedPackages = listOf(PACKAGE_FACEBOOK, PACKAGE_FACEBOOK_LITE),
        requireClickable = true,
        regionPreference = RegionPreference.TOP
    )

    // Final Post Settings Share / Post Button (Lesson 7 & 8)
    val PUBLISH_BUTTON = TargetSelector(
        exactTexts = listOf("POST", "Post", "SHARE", "Share"),
        exactDescriptions = listOf("Post", "Share", "Publish"),
        excludeTexts = listOf("Share to story", "Share to groups", "Save as draft", "Discard post"),
        allowedPackages = listOf(PACKAGE_FACEBOOK, PACKAGE_FACEBOOK_LITE),
        requireClickable = true,
        regionPreference = RegionPreference.TOP
    )

    // Success Indicators
    val SUCCESS_INDICATORS = TargetSelector(
        exactTexts = listOf("Posting...", "Post shared", "Published"),
        requireClickable = false
    )
}
