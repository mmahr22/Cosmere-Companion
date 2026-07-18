// Root build file. Plugin versions are managed in gradle/libs.versions.toml.
// Note: Android plugins are intentionally NOT declared here so that the pure
// JVM :core module can be built in environments without the Android SDK.
tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
