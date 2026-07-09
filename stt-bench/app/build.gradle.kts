plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val appVersionName = (findProperty("VERSION_NAME") as String?)
    ?: System.getenv("VERSION_NAME")
    ?: "0.3.9"
val appVersionCode = ((findProperty("VERSION_CODE") as String?)
    ?: System.getenv("VERSION_CODE")
    ?: "309").toInt()
val releaseKeystoreFile = System.getenv("ANDROID_KEYSTORE_FILE")

val githubRepositoryRaw = (findProperty("GITHUB_REPOSITORY") as String?)
    ?: System.getenv("GITHUB_REPOSITORY")
    ?: ""
val githubRepository = githubRepositoryRaw.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "dev.bitsbots.tldhbench"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.bitsbots.tldhbench"
        minSdk = 28
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resValue("string", "distribution_channel", "benchmark")
        buildConfigField("String", "GITHUB_REPOSITORY", "\"$githubRepository\"")
        vectorDrawables { useSupportLibrary = true }
    }

    if (!releaseKeystoreFile.isNullOrBlank()) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystoreFile)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (!releaseKeystoreFile.isNullOrBlank()) {
                signingConfigs.getByName("release")
            } else {
                // Benchmark app: keep CI/release APK installable even before a dedicated key exists.
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
        jniLibs { pickFirsts += "**/libc++_shared.so" }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2026.04.01"))
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // First real German/offline mobile STT candidate.
    implementation("com.alphacephei:vosk-android:0.3.47")

    // Second offline candidate: Android whisper.cpp wrapper with explicit language control.
    implementation("dev.ffmpegkit-maintained:whisper-android:1.0.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
}
