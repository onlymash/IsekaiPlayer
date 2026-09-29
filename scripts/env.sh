#!/bin/bash
# scripts/env.sh

# 基础路径定义
# 使用 BASH_SOURCE[0] 获取脚本自身路径，从而推算出项目根目录
# shellcheck disable=SC2155
export PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
export EXTERNAL_DIR="$PROJECT_ROOT/external"
export EXTERNAL_SRC_DIR="$EXTERNAL_DIR/sources"
export PREBUILT_DIR="$PROJECT_ROOT/external/prebuilt"

# 基础环境检查
if [ -z "$ANDROID_HOME" ]; then
    echo "Error: ANDROID_HOME is not set."
    exit 1
fi

# 版本管理 (从 gradle/config.versions.toml 统一读取)
VERSIONS_FILE="$PROJECT_ROOT/gradle/config.versions.toml"
if [ ! -f "$VERSIONS_FILE" ]; then
    echo "Error: $VERSIONS_FILE not found."
    exit 1
fi

NDK_VERSION=$(grep "android-ndk =" "$VERSIONS_FILE" | sed 's/.*"\(.*\)".*/\1/')
API_LEVEL=$(grep "android-minSdk =" "$VERSIONS_FILE" | sed 's/.*"\(.*\)".*/\1/')
export NDK_VERSION
export API_LEVEL

if [ -z "$NDK_VERSION" ] || [ -z "$API_LEVEL" ]; then
    echo "Error: Failed to parse NDK_VERSION or API_LEVEL from $VERSIONS_FILE"
    exit 1
fi

export NDK_PATH="$ANDROID_HOME/ndk/$NDK_VERSION"

if [ ! -d "$NDK_PATH" ]; then
    echo "Error: NDK version $NDK_VERSION not found at $NDK_PATH"
    exit 1
fi

# 架构映射
ARCH=$1
case $ARCH in
    arm64)
        export TARGET_TRIPLE="aarch64-linux-android"
        export ABI="arm64-v8a"
        export FF_ARCH="aarch64"
        export FF_CPU="armv8-a"
        ;;
    armv7l)
        export TARGET_TRIPLE="arm-linux-androideabi"
        export ABI="armeabi-v7a"
        export FF_ARCH="arm"
        export FF_CPU="armv7-a"
        ;;
    x86_64)
        export TARGET_TRIPLE="x86_64-linux-android"
        export ABI="x86_64"
        export FF_ARCH="x86_64"
        export FF_CPU="generic"
        ;;
    x86)
        export TARGET_TRIPLE="i686-linux-android"
        export ABI="x86"
        export FF_ARCH="x86"
        export FF_CPU="i686"
        ;;
    *) echo "Unsupported arch: $ARCH"; exit 1 ;;
esac

# 并行编译 CPU 核心数识别 (跨 Linux / macOS 兼容)
get_jobs_count() {
    if command -v nproc >/dev/null 2>&1; then
        nproc
    elif [ "$(uname)" = "Darwin" ]; then
        sysctl -n hw.ncpu 2>/dev/null || echo 4
    else
        echo 4
    fi
}
export NPROC=$(get_jobs_count)

# 工具链配置 (自动识别宿主机操作系统/架构，兼容 Linux 和 macOS)
HOST_TAG=$(basename "$(ls -d "$NDK_PATH/toolchains/llvm/prebuilt/"* 2>/dev/null | head -n 1)")
if [ -z "$HOST_TAG" ]; then
    echo "Error: Could not find prebuilt toolchain directory in $NDK_PATH/toolchains/llvm/prebuilt/"
    exit 1
fi
export TOOLCHAIN="$NDK_PATH/toolchains/llvm/prebuilt/$HOST_TAG"
export PATH="$TOOLCHAIN/bin:$PATH"

if [ "$ARCH" == "armv7l" ]; then
    export CC="armv7a-linux-androideabi${API_LEVEL}-clang"
    export CXX="armv7a-linux-androideabi${API_LEVEL}-clang++"
else
    export CC="${TARGET_TRIPLE}${API_LEVEL}-clang"
    export CXX="${TARGET_TRIPLE}${API_LEVEL}-clang++"
fi

export AR="llvm-ar"
export AS="$CC"
export RANLIB="llvm-ranlib"
export STRIP="llvm-strip"
export NM="llvm-nm"

# 编译参数
export CFLAGS="-Os -fPIC -ffunction-sections -fdata-sections -flto=thin -fstack-protector-strong -D_FORTIFY_SOURCE=2"
export CXXFLAGS="-Os -fPIC -ffunction-sections -fdata-sections -flto=thin -fstack-protector-strong -D_FORTIFY_SOURCE=2"

if [ "$ARCH" == "armv7l" ]; then
    export CFLAGS="$CFLAGS -mfpu=neon -mcpu=cortex-a8"
    export CXXFLAGS="$CXXFLAGS -mfpu=neon -mcpu=cortex-a8"
fi

export LDFLAGS="-Wl,-O1 -Wl,--icf=safe -Wl,--gc-sections -Wl,-z,max-page-size=16384 -Wl,--as-needed -flto=thin"

# 严格隔离 pkg-config (防止引用宿主机库)
export PKG_CONFIG_LIBDIR="$PREBUILT_DIR/$ABI/lib/pkgconfig"
export PKG_CONFIG_SYSROOT_DIR=""
export PKG_CONFIG_PATH=""
export PKG_CONFIG_ALLOW_SYSTEM_LIBS=0
export PKG_CONFIG_ALLOW_SYSTEM_CFLAGS=0
