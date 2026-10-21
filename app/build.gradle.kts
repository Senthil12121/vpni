plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.josvpn.india"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.josvpn.india"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
    }
}

dependencies {
    implementation("com.wireguard.android:tunnel:1.0.20250531")
}
