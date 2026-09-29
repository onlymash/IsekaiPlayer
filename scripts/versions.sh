#!/bin/bash

# 依赖库版本定义
export V_MPV="master"
export V_FFMPEG="master"
export V_LIBPLACEBO="master"
export V_LIBASS="master"
export V_LUA="v5.2.4"
export V_DAV1D="1.5.4"
export V_UNIBREAK="libunibreak_7_0"
export V_FRIBIDI="v1.0.16"
export V_FREETYPE="VER-2-14-3"
export V_LIBXML2="v2.15.4"
export V_FONTCONFIG="2.18.2"
export V_HARFBUZZ="14.3.1"
export V_CURL="curl-8_21_0"
export V_MBEDTLS="v3.6.7"
export V_LIBWEBP="v1.6.0"

# 依赖库源码仓库统一定义与遍历函数 (C/C++ Native)
# 参数: 回调函数名 (URL, 目录名, 版本号/引用, 是否递归:1/0, 名称, 项目URL, 许可证名称, 许可证URL, 描述信息)
for_each_repo() {
    local action=$1
    $action "https://github.com/mpv-player/mpv.git" "mpv" "$V_MPV" 0 \
        "mpv" "https://mpv.io" "LGPL-2.1-or-later" "https://github.com/mpv-player/mpv/blob/master/Copyright" \
        "Command-line and embeddable media player core engine"
    $action "https://github.com/FFmpeg/FFmpeg.git" "ffmpeg" "$V_FFMPEG" 0 \
        "FFmpeg" "https://ffmpeg.org" "LGPL-2.1-or-later" "https://ffmpeg.org/legal.html" \
        "Cross-platform solution for recording, converting, and streaming audio and video"
    $action "https://github.com/haasn/libplacebo.git" "libplacebo" "$V_LIBPLACEBO" 1 \
        "libplacebo" "https://code.videolan.org/videolan/libplacebo" "LGPL-2.1-or-later" "https://code.videolan.org/videolan/libplacebo/-/blob/master/LICENSE" \
        "Vulkan/OpenGL-based GPU image processing and color management library"
    $action "https://github.com/libass/libass.git" "libass" "$V_LIBASS" 0 \
        "libass" "https://github.com/libass/libass" "ISC" "https://github.com/libass/libass/blob/master/COPYING" \
        "Portable subtitle renderer for ASS/SSA subtitle format"
    $action "https://github.com/onlymash/lua.git" "lua" "$V_LUA" 0 \
        "Lua" "https://www.lua.org" "MIT" "https://www.lua.org/license.html" \
        "Lightweight embeddable scripting language engine"
    $action "https://github.com/videolan/dav1d.git" "dav1d" "$V_DAV1D" 0 \
        "dav1d" "https://code.videolan.org/videolan/dav1d" "BSD-2-Clause" "https://code.videolan.org/videolan/dav1d/-/blob/master/COPYING" \
        "Fast and efficient open-source AV1 video decoder"
    $action "https://github.com/fribidi/fribidi.git" "fribidi" "$V_FRIBIDI" 0 \
        "FriBidi" "https://github.com/fribidi/fribidi" "LGPL-2.1-or-later" "https://github.com/fribidi/fribidi/blob/master/COPYING" \
        "Implementation of the Unicode Bidirectional Algorithm (Bidi)"
    $action "https://github.com/adah1972/libunibreak.git" "unibreak" "$V_UNIBREAK" 0 \
        "libunibreak" "https://github.com/adah1972/libunibreak" "zlib/libpng" "https://github.com/adah1972/libunibreak/blob/master/LICENCE" \
        "Implementation of Unicode line breaking and word breaking algorithms"
    $action "https://github.com/harfbuzz/harfbuzz.git" "harfbuzz" "$V_HARFBUZZ" 0 \
        "HarfBuzz" "https://harfbuzz.github.io" "Old MIT" "https://github.com/harfbuzz/harfbuzz/blob/main/COPYING" \
        "Open-source text shaping and typography engine"
    $action "https://gitlab.freedesktop.org/fontconfig/fontconfig.git" "fontconfig" "$V_FONTCONFIG" 0 \
        "Fontconfig" "https://www.fontconfig.org" "MIT" "https://gitlab.freedesktop.org/fontconfig/fontconfig/-/blob/main/COPYING" \
        "Library for font configuration, customization, and matching"
    $action "https://gitlab.freedesktop.org/freetype/freetype.git" "freetype2" "$V_FREETYPE" 1 \
        "FreeType" "https://freetype.org" "FTL" "https://gitlab.freedesktop.org/freetype/freetype/-/blob/master/docs/FTL.TXT" \
        "High-performance software font rendering engine"
    $action "https://github.com/curl/curl.git" "curl" "$V_CURL" 0 \
        "curl" "https://curl.se" "curl License" "https://curl.se/docs/copyright.html" \
        "Command line tool and library for transferring data with URLs"
    $action "https://github.com/Mbed-TLS/mbedtls.git" "mbedtls" "$V_MBEDTLS" 1 \
        "mbedTLS" "https://github.com/Mbed-TLS/mbedtls" "Apache-2.0" "https://github.com/Mbed-TLS/mbedtls/blob/development/LICENSE" \
        "Lightweight C library for TLS/SSL and crypto primitives"
    $action "https://gitlab.gnome.org/GNOME/libxml2.git" "libxml2" "$V_LIBXML2" 0 \
        "libxml2" "https://gitlab.gnome.org/GNOME/libxml2" "MIT" "https://gitlab.gnome.org/GNOME/libxml2/-/blob/master/Copyright" \
        "C parser and toolkit for XML files and documents"
    $action "https://github.com/webmproject/libwebp.git" "libwebp" "$V_LIBWEBP" 0 \
        "libwebp" "https://chromium.googlesource.com/webm/libwebp" "BSD-3-Clause" "https://chromium.googlesource.com/webm/libwebp/+/refs/heads/main/COPYING" \
        "WebP image format library"
}

# App 层 JVM/Android 依赖库定义函数 (与 libs.versions.toml 实时联动)
# 参数: 回调函数名 (名称, 版本号, 项目URL, 许可证名称, 许可证URL, 描述信息)
for_each_app_repo() {
    local action=$1
    local project_root
    project_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
    local toml_file="$project_root/gradle/libs.versions.toml"

    get_toml_version() {
        local var_name=$1
        if [ -f "$toml_file" ]; then
            sed -n '/^\[versions\]/,/^\[/p' "$toml_file" | grep -E "^[[:space:]]*${var_name}[[:space:]]*=" | head -n 1 | cut -d'"' -f2
        fi
    }

    local v_media3
    v_media3=$(get_toml_version "androidx-media")
    local v_compose
    v_compose=$(get_toml_version "androidx-compose")
    local v_navigation3
    v_navigation3=$(get_toml_version "androidx-navigation3")
    local v_datastore
    v_datastore=$(get_toml_version "androidx-datastore")
    local v_room
    v_room=$(get_toml_version "androidx-room")
    local v_paging
    v_paging=$(get_toml_version "androidx-paging")
    local v_coroutines
    v_coroutines=$(get_toml_version "kotlinx-coroutines")
    local v_serialization
    v_serialization=$(get_toml_version "kotlinx-serialization")
    local v_koin
    v_koin=$(get_toml_version "koin")
    local v_ktor
    v_ktor=$(get_toml_version "ktor")
    local v_coil
    v_coil=$(get_toml_version "coil")
    local v_smbj
    v_smbj=$(get_toml_version "smbj")
    local v_dav4jvm
    v_dav4jvm=$(get_toml_version "dav4jvm")
    local v_commons_net
    v_commons_net=$(get_toml_version "commons-net")
    local v_okhttp
    v_okhttp=$(get_toml_version "okhttp")
    local v_backdrop
    v_backdrop=$(get_toml_version "backdrop")

    $action "AndroidX Media3 (ExoPlayer)" "${v_media3:-1.11.1}" "https://developer.android.com/media/media3" "Apache-2.0" "https://www.apache.org/licenses/LICENSE-2.0.txt" "AndroidX Media3 media playback, ExoPlayer engine"
    $action "Jetpack Compose" "${v_compose:-1.12.1}" "https://developer.android.com/jetpack/compose" "Apache-2.0" "https://www.apache.org/licenses/LICENSE-2.0.txt" "Android Jetpack modern UI toolkit for building native UI"
    $action "AndroidX Navigation3" "${v_navigation3:-1.1.7}" "https://developer.android.com/guide/navigation/navigation-3" "Apache-2.0" "https://www.apache.org/licenses/LICENSE-2.0.txt" "Navigation 3 is a navigation library designed to work with Compose"
    $action "AndroidX DataStore" "${v_datastore:-1.2.1}" "https://developer.android.com/topic/libraries/architecture/datastore" "Apache-2.0" "https://www.apache.org/licenses/LICENSE-2.0.txt" "Data store library for storing key-value pairs or typed objects with protocol buffers"
    $action "AndroidX Room3" "${v_room:-3.0.3}" "https://developer.android.com/jetpack/androidx/releases/room3" "Apache-2.0" "https://www.apache.org/licenses/LICENSE-2.0.txt" "The Room persistence library provides an abstraction layer over SQLite to allow for more robust database access while harnessing the full power of SQLite"
    $action "AndroidX Paging" "${v_paging:-3.5.1}" "https://developer.android.com/topic/libraries/architecture/paging/v3-overview" "Apache-2.0" "https://www.apache.org/licenses/LICENSE-2.0.txt" "The Paging library helps you load and display pages of data from larger datasets from local storage or over network"
    $action "Kotlin Coroutines" "${v_coroutines:-1.11.0}" "https://github.com/Kotlin/kotlinx.coroutines" "Apache-2.0" "https://github.com/Kotlin/kotlinx.coroutines/blob/master/LICENSE.txt" "Kotlin official asynchronous coroutines library"
    $action "Kotlin Serialization" "${v_serialization:-1.11.0}" "https://github.com/Kotlin/kotlinx.serialization" "Apache-2.0" "https://github.com/Kotlin/kotlinx.serialization/blob/master/LICENSE.txt" "Kotlin official cross-platform serialization library"
    $action "Koin" "${v_koin:-4.2.2}" "https://insert-koin.io" "Apache-2.0" "https://github.com/InsertKoinIO/koin/blob/main/LICENSE" "Pragmatic lightweight dependency injection framework for Kotlin & Android"
    $action "Ktor" "${v_ktor:-3.5.2}" "https://ktor.io" "Apache-2.0" "https://github.com/ktorio/ktor/blob/main/LICENSE" "Asynchronous multiplatform HTTP client and embedded server framework"
    $action "Coil" "${v_coil:-3.6.2}" "https://coil-kt.github.io/coil/" "Apache-2.0" "https://github.com/coil-kt/coil/blob/main/LICENSE.txt" "Image loading library for Android powered by Kotlin Coroutines"
    $action "smbj" "${v_smbj:-0.15.0}" "https://github.com/hierynomus/smbj" "Apache-2.0" "https://github.com/hierynomus/smbj/blob/master/LICENSE_HEADER" "SMB2 / SMB3 client library for Java and Kotlin"
    $action "dav4jvm" "${v_dav4jvm:-4.1.0}" "https://github.com/bitfireAT/dav4jvm" "MPL-2.0" "https://github.com/bitfireAT/dav4jvm/blob/main/LICENSE" "WebDAV client library for JVM and Android"
    $action "Apache Commons Net" "${v_commons_net:-3.13.0}" "https://commons.apache.org/proper/commons-net/" "Apache-2.0" "https://www.apache.org/licenses/LICENSE-2.0.txt" "Apache network protocol client library (FTP, FTPS, etc.)"
    $action "OkHttp" "${v_okhttp:-5.5.0}" "https://square.github.io/okhttp/" "Apache-2.0" "https://github.com/square/okhttp/blob/master/LICENSE.txt" "HTTP client for Java and Android applications"
    $action "Backdrop" "${v_backdrop:-2.0.1}" "https://github.com/Kyant0/AndroidLiquidGlass" "Apache-2.0" "https://github.com/kyant0/AndroidLiquidGlass/blob/kmp/LICENSE" "Offscreen glassmorphic backdrop blur and refraction visual effect library for Compose"
}
