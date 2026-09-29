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

package com.fiepi.media.player.utils

import android.content.Context
import android.content.res.AssetManager
import java.io.File

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object AssetUtils {
    private fun copyAssetFile(assetManager: AssetManager, filename: String, outFile: File): Boolean {
        var ins: InputStream? = null
        var out: OutputStream? = null
        try {
            ins = assetManager.open(filename, AssetManager.ACCESS_STREAMING)
            // Note that .available() will return the full file size for asset streams, and it even works
            // for compressed assets. Though none of this is documented...
            val avail = ins.available().toLong()
            if (outFile.length() == avail) {
                return true
            }
            out = FileOutputStream(outFile)
            ins.copyTo(out)
        } catch (_: IOException) {
            return false
        } finally {
            out?.close()
            ins?.close()
        }
        return true
    }

    /** Write the 'fonts.conf' for fontconfig. */
    private fun writeFontsConf(context: Context, configFile: File) {
        val parts = mutableListOf(
            "<fontconfig>",
            // Android system fonts reside here
            "<dir>/system/fonts/</dir>",
            "<dir>/product/fonts/</dir>",
            // Point fontconfig to the right cache path so that caching works
            "<cachedir>${context.cacheDir.path}</cachedir>",
            // Conveniently there is *no* Java API to query the system default fonts, but we can
            // manually specify the font families we know Android uses and provides by default.
            // (compare to 60-latin.conf shipped with fontconfig)
            "<alias><family>serif</family>",
            "<prefer><family>Noto Serif</family></prefer>",
            "</alias>",
            "<alias><family>sans-serif</family>",
            "<prefer>",
            "<family>Roboto</family>",
            "<family>Noto Sans</family>", // other languages
            "</prefer>",
            "</alias>",
            "<alias><family>monospace</family>",
            "<prefer><family>Droid Sans Mono</family></prefer>",
            "</alias>",
            "</fontconfig>"
        )
        try {
            configFile.writeText(parts.joinToString("\n"))
        } catch (_: IOException) {
        }
    }

    fun copyAssets(context: Context) {
        val configDir = context.filesDir.path
        val versionFile = File("$configDir/assets_version")
        
        val packageInfo = try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (_: Exception) {
            null
        }
        val currentVersion = packageInfo?.longVersionCode ?: 0L

        // Check version tag file
        val lastVersion = if (versionFile.exists()) {
            versionFile.readText().toLongOrNull() ?: -1L
        } else {
            -1L
        }

        val certFile = File("$configDir/cacert.pem")
        val fontsFile = File("$configDir/fonts.conf")

        // Copy only when version changes or core files are missing
        if (currentVersion == lastVersion && certFile.exists() && fontsFile.exists()) {
            return
        }

        val assetManager = context.assets
        val files = arrayOf("cacert.pem")

        for (name in files) {
            copyAssetFile(assetManager, name, File("$configDir/$name"))
        }

        writeFontsConf(context, fontsFile)

        // Update version tag file
        try {
            versionFile.writeText(currentVersion.toString())
        } catch (_: IOException) {
        }
    }

    fun findRealPath(fd: Int): String? {
        var ins: InputStream? = null
        try {
            val path = File("/proc/self/fd/${fd}").canonicalPath
            if (!path.startsWith("/proc") && File(path).canRead()) {
                // Double check that we can read it
                ins = FileInputStream(path)
                ins.read()
                return path
            }
        } catch(_: Exception) { } finally { ins?.close() }
        return null
    }
}