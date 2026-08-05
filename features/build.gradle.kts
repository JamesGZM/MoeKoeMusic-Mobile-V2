plugins {
    id("moekoe.android.library")
    id("moekoe.android.compose")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.screenshot)
}

android {
    namespace = "cn.james.music.features"
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    testOptions {
        screenshotTests {
            imageDifferenceThreshold = 0.001f
        }
    }
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
    implementation(projects.playback)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
    debugImplementation(libs.androidx.compose.ui.tooling)
    screenshotTestImplementation(platform(libs.androidx.compose.bom))
    screenshotTestImplementation(libs.androidx.compose.ui.tooling)
    screenshotTestImplementation(libs.screenshot.validation.api)
}
