// Root build configuration.
// Version pins for the whole build live here; modules reference the plugins by id only.
// - Gradle 9.6 (system Gradle, see wrapper)
// - AGP 9.5.1 requires Gradle 9.5+ and JDK 21 (Termux OpenJDK 21 matches)
// - Kotlin 2.3.21 (matches Kotlin bundled with this Gradle version)
plugins {
    id("com.android.application") version "9.5.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.21" apply false
}
