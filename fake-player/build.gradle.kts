// Development-only stand-in for a music app, used to test Media Auto-Skipper on an emulator
// without a Google/Spotify account. It reuses Spotify's package name so the default monitored
// list applies; uninstall it before installing the real Spotify.
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "fakeplayer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.spotify.music"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "test"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
