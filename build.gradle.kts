plugins {
    id("com.android.application") version "8.7.2" apply false
    id("com.android.library") version "8.7.2" apply false

    // Kotlin (must match your app + compose setup)
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false

    // Room compiler (KSP)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
}