// Capy Pomodoro is a single-module build: this project *is* the Android app.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.capypomodoro"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.capypomodoro"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Signed with the debug key so the release APK can be sideloaded straight away.
            // Swap for a real keystore before publishing to Google Play.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // SoundPool needs the chime uncompressed inside the APK.
        noCompress += listOf("ogg", "mp3", "m4a")
    }

    // Audio lives in the repo-level assets/audio folder (see HANDOFF.md). Files dropped there
    // are packaged as APK assets and referenced by file name from audio/Tracks.kt.
    sourceSets["main"].assets.srcDir("../assets/audio")

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.media3.exoplayer)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
