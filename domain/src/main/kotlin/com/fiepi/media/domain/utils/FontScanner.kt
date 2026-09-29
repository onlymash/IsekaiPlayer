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

package com.fiepi.media.domain.utils

import java.io.File
import java.io.RandomAccessFile
import java.nio.charset.StandardCharsets

data class FontInfo(
    val familyName: String,
    val fileName: String,
    val filePath: String
)

object FontScanner {

    /**
     * Scans the specified directory for font files (.ttf, .otf, .ttc) and returns a list of FontInfo
     */
    fun scanFontFamilies(dirPath: String): List<FontInfo> {
        val dir = File(dirPath)
        if (!dir.exists() || !dir.isDirectory) return emptyList()

        val fontFiles = dir.listFiles { _, name ->
            val lower = name.lowercase()
            lower.endsWith(".ttf") || lower.endsWith(".otf") || lower.endsWith(".ttc")
        } ?: return emptyList()

        val results = mutableListOf<FontInfo>()
        for (file in fontFiles.sortedBy { it.name.lowercase() }) {
            val familyName = extractFontFamily(file) ?: file.nameWithoutExtension
            results.add(
                FontInfo(
                    familyName = familyName,
                    fileName = file.name,
                    filePath = file.absolutePath
                )
            )
        }
        return results
    }

    /**
     * Extracts font family name from TTF/OTF OpenType 'name' table.
     */
    fun extractFontFamily(file: File): String? {
        if (!file.exists() || !file.canRead()) return null
        return try {
            RandomAccessFile(file, "r").use { raf ->
                parseFontFamilyName(raf)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseFontFamilyName(raf: RandomAccessFile): String? {
        val sfntVersion = raf.readInt()

        // Handle TTC (TrueType Collection) header
        val fontOffsets = if (sfntVersion == 0x74746366) { // 'ttcf'
            raf.skipBytes(4) // Version
            val numFonts = raf.readInt()
            val offsets = LongArray(numFonts)
            for (i in 0 until numFonts) {
                offsets[i] = raf.readInt().toLong() and 0xFFFFFFFFL
            }
            offsets
        } else {
            longArrayOf(0L)
        }

        for (offset in fontOffsets) {
            raf.seek(offset)
            val subVersion = raf.readInt()
            // Valid sfnt versions: 0x00010000, 0x4F54544F ('OTTO'), 'true', 'typ1'
            val numTables = raf.readUnsignedShort()
            raf.skipBytes(6) // searchRange, entrySelector, rangeShift

            var nameTableOffset = -1L
            for (i in 0 until numTables) {
                val tag = raf.readInt()
                val checkSum = raf.readInt()
                val tableOffset = raf.readInt().toLong() and 0xFFFFFFFFL
                val tableLength = raf.readInt().toLong() and 0xFFFFFFFFL

                if (tag == 0x6e616d65) { // 'name'
                    nameTableOffset = tableOffset
                    break
                }
            }

            if (nameTableOffset != -1L) {
                raf.seek(nameTableOffset)
                val format = raf.readUnsignedShort()
                val count = raf.readUnsignedShort()
                val stringOffset = raf.readUnsignedShort()

                val nameRecords = mutableListOf<NameRecord>()
                for (i in 0 until count) {
                    val platformId = raf.readUnsignedShort()
                    val encodingId = raf.readUnsignedShort()
                    val languageId = raf.readUnsignedShort()
                    val nameId = raf.readUnsignedShort()
                    val length = raf.readUnsignedShort()
                    val nameOffset = raf.readUnsignedShort()

                    nameRecords.add(
                        NameRecord(
                            platformId = platformId,
                            encodingId = encodingId,
                            languageId = languageId,
                            nameId = nameId,
                            length = length,
                            offset = nameOffset
                        )
                    )
                }

                val stringsBaseOffset = nameTableOffset + stringOffset
                var nameId16Result: String? = null
                var nameId1Result: String? = null

                for (record in nameRecords) {
                    if (record.nameId == 16 || record.nameId == 1) {
                        raf.seek(stringsBaseOffset + record.offset)
                        val bytes = ByteArray(record.length)
                        raf.readFully(bytes)
                        val decoded = decodeString(bytes, record.platformId, record.encodingId)
                        val trimmed = decoded?.trim()
                        if (!trimmed.isNullOrEmpty()) {
                            if (record.nameId == 16 && nameId16Result == null) {
                                nameId16Result = trimmed
                            } else if (record.nameId == 1 && nameId1Result == null) {
                                nameId1Result = trimmed
                            }
                        }
                    }
                }

                val finalName = nameId16Result ?: nameId1Result
                if (finalName != null) return finalName
            }
        }
        return null
    }

    private fun decodeString(bytes: ByteArray, platformId: Int, encodingId: Int): String? {
        return try {
            when (platformId) {
                0 -> String(bytes, StandardCharsets.UTF_16BE) // Unicode
                3 -> String(bytes, StandardCharsets.UTF_16BE) // Windows (Unicode BMP/Full)
                1 -> String(bytes, StandardCharsets.ISO_8859_1) // Macintosh
                else -> String(bytes, StandardCharsets.UTF_8)
            }
        } catch (_: Exception) {
            null
        }
    }

    private data class NameRecord(
        val platformId: Int,
        val encodingId: Int,
        val languageId: Int,
        val nameId: Int,
        val length: Int,
        val offset: Int
    )
}
