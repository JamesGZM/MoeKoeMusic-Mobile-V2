plugins {
    id("moekoe.android.library")
    id("moekoe.android.compose")
}

android {
    namespace = "cn.james.music.features"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
