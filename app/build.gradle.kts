import javax.inject.Inject
import org.gradle.process.ExecOperations

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val appVersionName = (findProperty("VERSION_NAME") as String?)
    ?: System.getenv("VERSION_NAME")
    ?: "0.7.1"
val appVersionCode = ((findProperty("VERSION_CODE") as String?)
    ?: System.getenv("VERSION_CODE")
    ?: "701").toInt()

/** NDK used to cross-compile whisper.cpp; CI installs exactly this version via sdkmanager. */
val whisperNdkVersion = "27.2.12479018"

/**
 * Cross-compiles whisper.cpp's `whisper-cli` for arm64-v8a via scripts/build-whisper-android.sh and
 * stages the variants as lib/arm64-v8a/libwhisper_cli*.so. The script pins the whisper.cpp tag and
 * commit and skips the build when its outputs are already current.
 */
abstract class BuildWhisperCli : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val script: RegularFileProperty

    @get:Input
    abstract val ndkDir: Property<String>

    @get:Internal
    abstract val workDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun build() {
        execOperations.exec {
            commandLine(
                "bash",
                script.get().asFile.absolutePath,
                ndkDir.get(),
                outputDir.get().asFile.absolutePath,
                workDir.get().asFile.absolutePath
            )
        }
    }
}

val buildWhisperCli = tasks.register<BuildWhisperCli>("buildWhisperCli") {
    script.set(rootProject.layout.projectDirectory.file("scripts/build-whisper-android.sh"))
    ndkDir.set(androidComponents.sdkComponents.sdkDirectory.map { it.dir("ndk/$whisperNdkVersion").asFile.absolutePath })
    workDir.set(layout.buildDirectory.dir("whisper-native/work"))
    outputDir.set(layout.buildDirectory.dir("whisper-native/jniLibs"))
}
val releaseKeystoreFile = System.getenv("ANDROID_KEYSTORE_FILE")

val githubRepositoryRaw = (findProperty("GITHUB_REPOSITORY") as String?)
    ?: System.getenv("GITHUB_REPOSITORY")
    ?: ""
val githubRepository = githubRepositoryRaw.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "dev.bitsbots.tldh"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.bitsbots.tldh"
        minSdk = 28
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GITHUB_REPOSITORY", "\"$githubRepository\"")
        buildConfigField("boolean", "UPDATER_ENABLED", "true")
        resValue("string", "distribution_channel", "stable")

        vectorDrawables {
            useSupportLibrary = true
        }
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
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (!releaseKeystoreFile.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
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
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            // whisper-cli is an executable, not a loadable library: it must be extracted to disk on
            // install; it is stripped by the build script, so AGP's own strip step must leave it alone.
            useLegacyPackaging = true
            keepDebugSymbols += "**/libwhisper_cli*.so"
        }
    }
}

androidComponents {
    onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(buildWhisperCli, BuildWhisperCli::outputDir)
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

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    // Real org.json for JVM unit tests; android.jar only ships stubs.
    testImplementation("org.json:json:20240303")

    androidTestImplementation(platform("androidx.compose:compose-bom:2026.04.01"))
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
