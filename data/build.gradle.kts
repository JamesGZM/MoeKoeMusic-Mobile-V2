plugins {
    id("moekoe.android.library")
}

android {
    namespace = "cn.james.music.data"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.kugouApi)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    testImplementation(libs.junit)
}
