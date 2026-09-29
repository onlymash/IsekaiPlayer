import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.fiepi.media.data"

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

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)

    implementation(libs.koin.core)
    implementation(libs.koin.android)

    implementation(libs.bundles.ktor)
    implementation(libs.bundles.kotlinx)

    implementation(libs.okhttp3)

    implementation(libs.smbj)
    implementation(libs.commons.net)
    implementation(libs.dav4jvm) {
        exclude(group = "org.ogce", module = "xpp3")
        exclude(group = "xmlpull", module = "xmlpull")
    }

    testImplementation(libs.test.junit)
    testImplementation(libs.test.kotlin.test)
}
