pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        gradlePluginPortal()
    }
}

rootProject.name = "dmcl"
rootProject.buildFileName = if (providers.gradleProperty("mc26").isPresent) "build26.gradle.kts" else "build.gradle.kts"
