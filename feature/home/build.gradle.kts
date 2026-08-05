plugins {
    id("moekoe.android.library")
    id("moekoe.android.compose")
    alias(libs.plugins.kotlin.serialization)
}

android { namespace = "cn.james.music.feature.home" }

dependencies {
    implementation(projects.core.designsystem)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
}
