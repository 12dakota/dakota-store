package com.dakota.store

data class RepoConfig(
    val owner: String,
    val repo: String,
    val displayName: String,
    val description: String
)

object AppList {
    val repos = listOf(
        RepoConfig(
            owner = "12dakota",
            repo = "provonlong-web-browser",
            displayName = "Provonlong Web Browser",
            description = "Multi-engine Android browser (Chromium + Gecko + WebKit)"
        ),
        // Add more of your repos here. Example:
        // RepoConfig("12dakota", "your-other-app", "Your App", "Description")
    )
}
