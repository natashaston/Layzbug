pluginManagement {
    repositories {
        // We removed the "content" filters to let Gradle find KSP and Hilt freely
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

rootProject.name = "Layzbug"
include(":app")