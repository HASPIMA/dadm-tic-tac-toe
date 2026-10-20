import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

// Opt-in: host of the local Firebase emulators for debug builds, e.g. 10.0.2.2.
// Set `firebase.emulatorHost` in local.properties or pass -Pfirebase.emulatorHost=...
// Unset (the default), the app uses the production Firebase project.
val firebaseEmulatorHost: String? =
    (findProperty("firebase.emulatorHost") as String?)
        ?: rootProject.file("local.properties").takeIf { it.exists() }?.let { file ->
            Properties().apply { file.inputStream().use(::load) }
                .getProperty("firebase.emulatorHost")
        }

android {
    namespace = "com.edu.unal.tictactoe"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.edu.unal.tictactoe"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = "2.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FIREBASE_EMULATOR_HOST", "null")
        manifestPlaceholders["usesCleartextTraffic"] = false
    }

    buildTypes {
        debug {
            if (!firebaseEmulatorHost.isNullOrBlank()) {
                buildConfigField("String", "FIREBASE_EMULATOR_HOST", "\"$firebaseEmulatorHost\"")
                // The emulators only speak plain HTTP
                manifestPlaceholders["usesCleartextTraffic"] = true
            }
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Import the Firebase BoM
    implementation(platform(libs.firebase.bom))

    // When using the BoM, don't specify versions in Firebase dependencies
    // https://firebase.google.com/docs/android/setup#available-libraries
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.functions)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.play.services)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}