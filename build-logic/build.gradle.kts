plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "cn.james.music.buildlogic"

dependencies {
    implementation("com.android.tools.build:gradle:9.3.0")
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
    }
}
