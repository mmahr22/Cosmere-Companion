pluginManagement {
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

rootProject.name = "CosmereCompanion"

include(":core")

// The Android app module requires the Android SDK. Skip it in headless
// environments (like CI sandboxes) that only build/test the :core module.
val localProperties = File(rootDir, "local.properties")
val hasAndroidSdk =
    System.getenv("ANDROID_HOME") != null ||
        System.getenv("ANDROID_SDK_ROOT") != null ||
        (localProperties.exists() && localProperties.readLines().any { it.startsWith("sdk.dir") })

if (hasAndroidSdk) {
    include(":app")
}
