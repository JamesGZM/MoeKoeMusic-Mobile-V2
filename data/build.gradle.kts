plugins {
    id("moekoe.android.library")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "cn.james.music.data"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.kugouApi)
    implementation(projects.playback)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
}
