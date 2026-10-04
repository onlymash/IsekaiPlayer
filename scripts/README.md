[中文说明 (Chinese Version)](README_zh.md)

# IsekaiPlayer Build & Utility Scripts Guide

This directory contains scripts responsible for managing, configuring, updating, and compiling IsekaiPlayer's native C/C++ dependencies (including `libmpv`, `FFmpeg`, `libplacebo`, `libass`, etc.), as well as generating metadata for third-party open-source components.

---

## 📁 Directory Structure

| File / Directory | Description |
| --- | --- |
| `build-native.sh` | Main entry point script for building native libraries and cleanup (supports smart argument parsing, flexible order, and ABI aliases) |
| `env.sh` | Environment configuration script (parses NDK version and MinSDK from `gradle/config.versions.toml`, configures cross-compilation toolchains and flags) |
| `versions.sh` | Centralized definition of native dependency Git repositories, tags/branches, licenses, and metadata, linked with `gradle/libs.versions.toml` |
| `sync-sources.sh` | Idempotent script to clone, sync, and update native dependency source code in `external/sources/` (supports single component target and fast mode) |
| `update-cacert.sh` | Utility script to download and update the Mozilla CA certificate bundle at `player/src/main/assets/cacert.pem` with SHA256 verification |
| `utils.sh` | Helper functions (Meson cross-file generator, `vulkan.pc` generator, and `libraries.json` generator) |
| `components/` | Individual build recipes for C/C++ dependencies (`mbedtls.sh`, `lua.sh`, `ffmpeg.sh`, `mpv.sh`, etc., 16 components in total) |

---

## 🛠️ Prerequisites & Environment Setup

Before running the build scripts, ensure your environment meets the following requirements:

### 1. Environment Variables
- `ANDROID_HOME`: Must be configured to point to your local Android SDK installation directory (e.g., `export ANDROID_HOME=$HOME/Android/Sdk`).

### 2. NDK Version Match
- The scripts automatically read `android-ndk` and `android-minSdk` from `gradle/config.versions.toml`.
- Ensure that the `$ANDROID_HOME/ndk/<NDK_VERSION>` directory exists.

### 3. Host Toolchain Dependencies
Supports Linux / macOS host environments with the following CLI tools installed:
- **Git** / **Bash**
- **Build Tools**: `make`, `cmake`, `meson`, `ninja`, `pkg-config`, `autoconf`, `automake`, `libtool`, `gperf`, `yasm`, `nasm`

---

## 🚀 Native Build Script (`build-native.sh`) Guide

`build-native.sh` features smart argument normalization, supporting zero-argument defaults, flexible argument order, and common ABI aliases.

### Command Syntax

```bash
./scripts/build-native.sh [arch] [action]
./scripts/build-native.sh [action] [arch]
# Special commands:
./scripts/build-native.sh clean [--src] [--so]
./scripts/build-native.sh libraries
./scripts/build-native.sh -h | --help
```

---

### Parameter Reference

#### 1. Architecture Argument `[arch]`

| `arch` Parameter | Aliases | Target Android ABI | Notes |
| --- | --- | --- | --- |
| `arm64` *(default)* | `arm64-v8a` | `arm64-v8a` | 64-bit ARM architecture |
| `armv7l` | `armeabi-v7a`, `armv7`, `armv7a` | `armeabi-v7a` | 32-bit ARM architecture |
| `x86` | `x86` | `x86` | 32-bit x86 emulator architecture |
| `x86_64` | `x86_64` | `x86_64` | 64-bit x86_64 emulator architecture |
| `32` | - | `armeabi-v7a` + `x86` | Batch builds all 32-bit architectures |
| `64` | - | `arm64-v8a` + `x86_64` | Batch builds all 64-bit architectures |
| `all` | - | All 4 ABIs | Batch builds all 4 supported architectures |

#### 2. Component / Action Argument `[action]`

| `action` Parameter | Description |
| --- | --- |
| `all` *(default)* | Builds all 16 C/C++ components sequentially in dependency order |
| Exact Component Name | Builds a single specific component. Allowed values:<br>`mbedtls`, `lua`, `dav1d`, `libxml2`, `freetype2`, `fribidi`, `harfbuzz`, `unibreak`, `fontconfig`, `libass`, `curl`, `libwebp`, `shaderc`, `ffmpeg`, `libplacebo`, `mpv` |

---

### Usage Examples

#### 1) Basic & Default Builds
```bash
# Default build: running with no arguments builds all components for arm64-v8a
./scripts/build-native.sh

# Component only: builds specified component for default arm64-v8a
./scripts/build-native.sh ffmpeg
./scripts/build-native.sh mpv

# Architecture only: builds all components for specified architecture
./scripts/build-native.sh arm64
./scripts/build-native.sh armeabi-v7a
./scripts/build-native.sh all
```

#### 2) Combined Builds (Bidirectional Order Support)
```bash
# Standard order [arch] [action]
./scripts/build-native.sh arm64 ffmpeg
./scripts/build-native.sh armv7l mpv

# Flexible order [action] [arch]
./scripts/build-native.sh ffmpeg arm64
./scripts/build-native.sh mpv 64
```

#### 3) Cleanup Commands
```bash
# Full clean (intermediate source build files + output prebuilt binaries)
./scripts/build-native.sh clean

# Clean intermediate source build artifacts only (build_android, config.log, etc.)
./scripts/build-native.sh clean --src

# Clean compiled output binaries in external/prebuilt/ only
./scripts/build-native.sh clean --so
```

#### 4) Open Source Metadata Generation
```bash
# Update external/prebuilt/assets/libraries.json only without triggering C/C++ compilation
./scripts/build-native.sh libraries
```

---

## 🔄 Source Management Script (`sync-sources.sh`)

The unified `sync-sources.sh` handles cloning, resetting build patches, pulling updates, checking out target tags/branches, and updating submodules for native repositories defined in `versions.sh`.

### Command Syntax

```bash
./scripts/sync-sources.sh [options] [target]
```

### Options & Target Reference

- `target`: Specific component name (e.g. `ffmpeg`, `mpv`, `libass`) or `all` (default).
- `-d, --download-only, --fast`: Fast mode. Only clones missing repositories and skips existing ones without network fetching or resetting local changes.
- `-h, --help`: Displays help message.

### Usage Examples

```bash
# Idempotently sync/update all native source repositories
./scripts/sync-sources.sh

# Sync/update a single component (e.g., ffmpeg only)
./scripts/sync-sources.sh ffmpeg

# Fast mode: download only missing repositories without pulling updates for existing ones
./scripts/sync-sources.sh --download-only

# Fast mode for a single component
./scripts/sync-sources.sh --fast mpv
```

---

## 🧱 C/C++ Component Build Dependency Order

When running with the `all` action, `build-native.sh` builds components sequentially in topological order, outputting binaries to `external/prebuilt/<ABI>/`:

1. **`mbedtls`**: Cryptography and TLS protocol library
2. **`lua`**: Scripting engine for mpv script execution
3. **`dav1d`**: AV1 video decoder
4. **`libxml2`**: XML parsing toolkit
5. **`freetype2`**: Font rendering engine
6. **`fribidi`**: Unicode Bidirectional algorithm library (Bidi)
7. **`harfbuzz`**: Text shaping engine
8. **`unibreak`**: Unicode line and word breaking algorithm library
9. **`fontconfig`**: Font configuration and matching library
10. **`libass`**: Subtitle rendering engine
11. **`curl`**: URL network transfer library
12. **`libwebp`**: WebP image format decoding and encoding library
13. **`shaderc`**: Vulkan SPIR-V shader compiler
14. **`ffmpeg`**: Audio/video demuxing and decoding (with MediaCodec, JNI, mbedTLS, dav1d, libxml2, libwebp)
15. **`libplacebo`**: Vulkan/OpenGL-based GPU image rendering and color management
16. **`mpv`**: `libmpv` core media player library
