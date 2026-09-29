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
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.fiepi.media.player"

    buildToolsVersion = config.versions.android.buildTools.get()

    compileSdk {
        val sdk = config.versions.android.compileSdk.get()
        version = release(sdk.substringBefore('.').toInt()) {
            sdk.substringAfter('.', "").toIntOrNull()?.let { minorApiLevel = it }
        }
    }

    defaultConfig {
        minSdk = config.versions.android.minSdk.get().toInt()
    }

    sourceSets {
        getByName("main") {
            assets.directories += listOf("../external/prebuilt/assets/")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(config.versions.jdk.get())
        targetCompatibility = JavaVersion.toVersion(config.versions.jdk.get())
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    jvmToolchain(config.versions.jdk.get().toInt())
    compilerOptions {
        jvmTarget = JvmTarget.fromTarget(config.versions.jdk.get())
    }
}

dependencies {
    implementation(project(":libmpv"))
    implementation(project(":libffmpeg"))
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.koin.core)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)

    implementation(libs.bundles.androidx.media)
    implementation(libs.coil.core)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.auth)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.okhttp3)

    testImplementation(libs.test.junit)
    testImplementation(libs.test.mockk)
    testImplementation(libs.test.kotlinx.coroutines.test)
    testImplementation(libs.test.kotlin.test)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
