plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Workaround: AGP's per-library dexing cannot desugar the Java records used inside
// the WireGuard tunnel library when minSdk < 35. Building the debug APK through R8
// avoids that step. These rules keep every class and turn off renaming, so nothing
// is removed or obfuscated.
file("debug-rules.pro").writeText(
    "-keep class ** { *; }\n" +
        "-dontobfuscate\n" +
        "-dontoptimize\n" +
        "-dontwarn **\n"
)

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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            proguardFiles(file("debug-rules.pro"))
        }
    }
}

dependencies {
    implementation("com.wireguard.android:tunnel:1.0.20250531")
}
