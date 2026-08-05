plugins {
    id("moekoe.android.library")
    alias(libs.plugins.ksp)
}

android {
    namespace = "cn.james.music.core.database"
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.junit)
}
