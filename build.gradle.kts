plugins {
    id("moekoe.quality-gates")
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.screenshot) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**")
        ktlint()
        suppressLintsFor {
            step = "ktlint"
            shortCode = "standard:function-naming"
        }
    }
    kotlinGradle {
        target("**/*.gradle.kts")
        targetExclude("**/build/**")
        ktlint()
    }
    format("misc") {
        target("*.md", "docs/**/*.md", ".github/**/*.yml", ".github/**/*.yaml", "**/*.json")
        targetExclude("**/build/**")
        trimTrailingWhitespace()
        endWithNewline()
    }
}
