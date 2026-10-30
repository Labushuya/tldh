import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val appVersionName = (findProperty("VERSION_NAME") as String?)
    ?: System.getenv("VERSION_NAME")
    ?: "0.6.0"
val appVersionCode = ((findProperty("VERSION_CODE") as String?)
    ?: System.getenv("VERSION_CODE")
    ?: "600").toInt()

/**
 * Downloads the prebuilt Cactus `needle` engine (Apache-2.0) for Whistle from a pinned Hugging Face
 * revision, verifies its SHA-256 and stages it as lib/arm64-v8a/libneedle_cli.so. The lib*.so name lets
 * Android extract it into the app's native library dir, the one app location it may execute from.
 */
abstract class FetchNeedleRuntime : DefaultTask() {
    @get:Input
    abstract val url: Property<String>

    @get:Input
    abstract val sha256: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun fetch() {
        val target = outputDir.file("arm64-v8a/libneedle_cli.so").get().asFile
        target.parentFile.mkdirs()
        if (target.isFile && sha256Of(target.readBytes()) == sha256.get()) return
        var location = url.get()
        var bytes: ByteArray? = null
        for (redirect in 0 until 5) {
            val connection = URI(location).toURL().openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 30_000
            connection.readTimeout = 120_000
            val code = connection.responseCode
            if (code in 300..399) {
                location = URI(location).resolve(connection.getHeaderField("Location")).toString()
                connection.disconnect()
                continue
            }
            if (code !in 200..299) throw GradleException("needle download failed: HTTP $code from $location")
            bytes = connection.inputStream.use { it.readBytes() }
            break
        }
        val downloaded = bytes ?: throw GradleException("needle download failed: too many redirects")
        val actual = sha256Of(downloaded)
        if (actual != sha256.get()) throw GradleException("needle SHA-256 mismatch: expected ${sha256.get()}, got $actual")
        target.writeBytes(downloaded)
        target.setExecutable(true, false)
    }

    private fun sha256Of(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}

val fetchNeedleRuntime = tasks.register<FetchNeedleRuntime>("fetchNeedleRuntime") {
    url.set("https://huggingface.co/Cactus-Compute/needle3/resolve/d6c7d6a2e9655505e0f8e23339356dd1705900eb/android-arm64/needle")
    sha256.set("02a044e15489e19fa9f4f1cd1d5943d39dc0eefc08e0ba99d36279daf2351d46")
    outputDir.set(layout.buildDirectory.dir("generated/needle/jniLibs"))
}
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
        jniLibs {
            pickFirsts += "**/libc++_shared.so"
            // The Whistle needle engine is an executable, not a loadable library: it must be extracted
            // to disk on install and must not be touched by symbol stripping.
            useLegacyPackaging = true
            keepDebugSymbols += "**/libneedle_cli.so"
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(fetchNeedleRuntime, FetchNeedleRuntime::outputDir)
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
