import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.fiepi.media.app"

    buildToolsVersion = config.versions.android.buildTools.get()
    ndkVersion = config.versions.android.ndk.get()

    compileSdk {
        val sdk = config.versions.android.compileSdk.get()
        version = release(sdk.substringBefore('.').toInt()) {
            sdk.substringAfter('.', "").toIntOrNull()?.let { minorApiLevel = it }
        }
    }

    defaultConfig {
        applicationId = "com.fiepi.media.app"
        minSdk = config.versions.android.minSdk.get().toInt()
        targetSdk = config.versions.android.targetSdk.get().toInt()
        versionCode = config.versions.app.versionCode.get().toInt()
        versionName = config.versions.app.versionName.get()
        versionNameSuffix = "." + providers.of(GitHashValueSource::class.java) {}.getOrElse("nogit")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    splits {
        abi {
            isEnable = true
            isUniversalApk = false
            reset()
            listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64").onEach { abiName ->
                if (File(rootProject.projectDir, "external/prebuilt/$abiName").exists()) {
                    include(abiName)
                }
            }
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("../keystore/debug/debug.jks")
            storePassword = "password"
            keyAlias = "debug"
            keyPassword = "password"
        }

        create("release") {
            loadReleaseSigningInfo(rootProject.file("keystore/release"))?.let { info ->
                storeFile = info.storeFile
                keyAlias = info.keyAlias
                storePassword = info.storePassword
                keyPassword = info.keyPassword
            }
        }
    }

    buildTypes {
        release {
            optimization {
                enable = true
            }
            val releaseSigning = signingConfigs.findByName("release")
            signingConfig =
                if (releaseSigning?.storeFile != null && releaseSigning.storeFile?.exists() == true) {
                    releaseSigning
                } else {
                    signingConfigs.getByName("debug")
                }
        }
        debug {
            applicationIdSuffix = ".debug"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(config.versions.jdk.get())
        targetCompatibility = JavaVersion.toVersion(config.versions.jdk.get())
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

base {
    archivesName =
        "${rootProject.name}-${android.defaultConfig.versionName}${android.defaultConfig.versionNameSuffix}"
}

kotlin {
    jvmToolchain(config.versions.jdk.get().toInt())
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(config.versions.jdk.get())
    }
}

dependencies {
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":player"))

    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.paging.compose)

    implementation(libs.backdrop)

    implementation(libs.bundles.androidx.compose)
    implementation(libs.bundles.androidx.lifecycle)
    implementation(libs.bundles.coil)
    implementation(libs.bundles.koin)
    implementation(libs.bundles.kotlinx)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.turbine)
    testImplementation(libs.test.kotlinx.coroutines.test)
    testImplementation(libs.test.kotlin.test)
    testImplementation(libs.test.kotlin.testJunit)
    androidTestImplementation(libs.test.androidx.espresso.core)
    androidTestImplementation(libs.test.androidx.testExt.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

abstract class GitHashValueSource : ValueSource<String, ValueSourceParameters.None> {
    @get:Inject
    abstract val execOperations: ExecOperations

    override fun obtain(): String? {
        return try {
            val output = ByteArrayOutputStream()
            execOperations.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
                standardOutput = output
                isIgnoreExitValue = true
            }
            output.toString().trim().takeIf { it.isNotEmpty() }
        } catch (_: Exception) {
            null
        }
    }
}

private data class SigningInfo(
    val storeFile: File,
    val keyAlias: String,
    val storePassword: String,
    val keyPassword: String
)

private fun resolvePath(path: String): File {
    val resolved = if (path.startsWith("~")) {
        path.replaceFirst("~", System.getProperty("user.home"))
    } else {
        path
    }
    return File(resolved)
}

/**
 *  Loads release signing configuration from a pointer file in the release keystore directory.
 *
 *  Purpose:
 *  Prevents sensitive release signing credentials and keystore paths from being committed to source control.
 *
 *  Pointer File Format (keystore/release/filename.txt):
 *  Contains a single line specifying the path to the external .properties file.
 *  Example in keystore/release/path.txt:
 *  ~/secrets/signing.properties
 *
 *  External Configuration File Format (signing.properties):
 *  Standard Java Properties file containing signing credentials:
 *  storeFile=/path/to/release.jks
 *  keyAlias=my_alias
 *  storePassword=my_store_password
 *  keyPassword=my_key_password
 */
private fun loadReleaseSigningInfo(releaseDir: File): SigningInfo? {
    if (!releaseDir.exists()) return null
    val pointerFile =
        releaseDir.listFiles()?.firstOrNull { it.isFile && it.extension == "txt" } ?: return null
    val externalPath =
        pointerFile.readLines().firstOrNull { it.trim().isNotEmpty() }?.trim() ?: return null
    val externalFile = resolvePath(externalPath)
    if (!externalFile.exists()) return null

    val props = Properties()
    externalFile.inputStream().use { stream ->
        props.load(stream)
    }

    val storeFilePath = props.getProperty("storeFile")
        ?: props.getProperty("storeFilePath")
        ?: props.getProperty("path")
    val alias = props.getProperty("keyAlias")
        ?: props.getProperty("alias")
    val storePass = props.getProperty("storePassword")
        ?: props.getProperty("storePass")
        ?: props.getProperty("password")
    val keyPass = props.getProperty("keyPassword")
        ?: props.getProperty("keyPass")
        ?: storePass

    if (storeFilePath.isNullOrEmpty() || alias.isNullOrEmpty() || storePass.isNullOrEmpty()) return null
    val jksFile = resolvePath(storeFilePath)
    if (!jksFile.exists()) return null

    return SigningInfo(
        storeFile = jksFile,
        keyAlias = alias,
        storePassword = storePass,
        keyPassword = keyPass ?: storePass
    )
}