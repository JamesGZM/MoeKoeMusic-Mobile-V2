pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MoeKoeMusic-Mobile-V2"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    ":app",
    ":core:model",
    ":core:common",
    ":core:designsystem",
    ":core:database",
    ":kugou-api",
    ":data",
    ":playback",
    ":feature:home",
    ":feature:discover",
    ":feature:my",
    ":feature:search",
    ":feature:localmusic",
    ":feature:foundation",
)
