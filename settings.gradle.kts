pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven (url = uri("https://chaquo.com/maven-test") )
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DriveAide"
include(":app")
 