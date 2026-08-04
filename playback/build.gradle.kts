plugins {
    id("moekoe.android.library")
}

android {
    namespace = "cn.james.music.playback"
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    testImplementation(libs.junit)
}
