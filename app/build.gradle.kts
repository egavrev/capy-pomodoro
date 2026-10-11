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

    // Release signing: a permanent key kept outside the repo (CI gets it from repository secrets).
    // Without it, release builds fall back to the debug key — fine for testing, but every machine's
    // debug key differs, so installed copies couldn't be updated in place.
    val releaseKeystore = System.getenv("CAPY_KEYSTORE_FILE")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = System.getenv("CAPY_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("CAPY_KEY_ALIAS") ?: "capy"
                keyPassword = System.getenv("CAPY_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    androidResources {
        // SoundPool needs the chime uncompressed inside the APK.
        noCompress += listOf("ogg", "mp3", "m4a")
        // Default ignore list plus the README that sits next to the audio files.
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:<dir>_*:!CVS:!thumbs.db:!picasa.ini:!*~:!README.md"
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
