plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "cn.james.music.buildlogic"

dependencies {
    implementation(libs.android.tools.gradle)
    testImplementation(libs.junit)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "moekoe.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "moekoe.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "moekoe.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("qualityGates") {
            id = "moekoe.quality-gates"
            implementationClass = "MoeKoeQualityGatesPlugin"
        }
    }
}
