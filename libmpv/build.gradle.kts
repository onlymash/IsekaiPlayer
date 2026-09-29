/*
 * IsekaiPlayer - Sovereign above myriad realms; shatter every mortal cipher.
 * Copyright (C) 2026 onlymash
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:Suppress("UnstableApiUsage")

import org.jetbrains.kotlin.gradle.dsl.JvmTarget


plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.fiepi.mpv"

    buildToolsVersion = config.versions.android.buildTools.get()
    ndkVersion = config.versions.android.ndk.get()

    compileSdk {
        val sdk = config.versions.android.compileSdk.get()
        version = release(sdk.substringBefore('.').toInt()) {
            sdk.substringAfter('.', "").toIntOrNull()?.let { minorApiLevel = it }
        }
    }

    defaultConfig {
        minSdk = config.versions.android.minSdk.get().toInt()

        ndk {
            listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64").onEach { abiName ->
                if (File(rootProject.projectDir, "external/prebuilt/$abiName").exists()) {
                    abiFilters.add(abiName)
                }
            }
        }

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17")
                arguments("-DANDROID_STL=c++_shared")
            }
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.directories.add(("../../external/prebuilt"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "consumer-rules.keep"
            )
        }
    }

    externalNativeBuild {
        cmake {
            path("src/main/cpp/CMakeLists.txt")
            version = config.versions.android.cmake.get()
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(config.versions.jdk.get())
        targetCompatibility = JavaVersion.toVersion(config.versions.jdk.get())
    }
}

kotlin {
    jvmToolchain(config.versions.jdk.get().toInt())
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(config.versions.jdk.get())
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
}