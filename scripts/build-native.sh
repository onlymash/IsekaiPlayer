#!/bin/bash
set -e

# --- 基础路径与环境初始化 ---
PROJECT_ROOT=$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)
export PROJECT_ROOT
export EXTERNAL_DIR="$PROJECT_ROOT/external"
export EXTERNAL_SRC_DIR="$EXTERNAL_DIR/sources"
export PREBUILT_DIR="$PROJECT_ROOT/external/prebuilt"

# 加载辅助工具与组件构建脚本
source "$PROJECT_ROOT/scripts/utils.sh"
for script in "$PROJECT_ROOT/scripts/components"/*.sh; do
    # shellcheck disable=SC1090
    source "$script"
done

# --- 帮助文档 ---
show_help() {
    echo "Native Library Build Script"
    echo ""
    echo "Usage:"
    echo "  $0 [arch] [action]     - Build specific architecture and action"
    echo "  $0 [action] [arch]     - Flexible syntax support"
    echo "  $0 clean [options]     - Global cleanup"
    echo ""
    echo "Architectures (arch):"
    echo "  arm64        - Build for arm64-v8a (default)"
    echo "  armv7l       - Build for armeabi-v7a (aliases: armeabi-v7a, armv7)"
    echo "  x86          - Build for x86"
    echo "  x86_64       - Build for x86_64"
    echo "  32           - Build both armv7l and x86"
    echo "  64           - Build both arm64 and x86_64"
    echo "  all          - Build all supported architectures"
    echo ""
    echo "Actions (action):"
    echo "  all          - Build all native components (default)"
    echo "  libraries    - Generate libraries.json from versions.sh"
    echo "  <component>  - Build a specific component (e.g., ffmpeg, mpv, lua...)"
    echo ""
    echo "Cleanup Options (for '$0 clean'):"
    echo "  --src        - Only clean intermediate source artifacts (build_android, config.h...)"
    echo "  --so         - Delete all compiled libraries in external/prebuilt/"
    echo "  (no flags)   - Performs both --src and --so"
    echo ""
    echo "Options:"
    echo "  -h, --help   - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                   # Default: Build everything for arm64-v8a"
    echo "  $0 ffmpeg            # Build ffmpeg for default arm64-v8a"
    echo "  $0 arm64 ffmpeg      # Build ffmpeg for arm64-v8a"
    echo "  $0 all               # Build everything for all architectures"
    echo "  $0 clean             # Full cleanup of source and prebuilts"
}

# --- 清理逻辑 ---
clean_src() {
    local target=$1
    echo "--- Cleaning intermediate source artifacts ---"
    rm -f "$PROJECT_ROOT/cross_file.txt"

    local dirs=""
    if [ -z "$target" ] || [ "$target" == "all" ]; then
        dirs="mbedtls lua dav1d libxml2 freetype2 fribidi harfbuzz unibreak fontconfig libass curl libwebp ffmpeg libplacebo mpv shaderc_build"
    elif [ "$target" == "shaderc" ]; then
        dirs="shaderc_build"
    else
        dirs="$target"
    fi

    for dir in $dirs; do
        if [ -d "$EXTERNAL_SRC_DIR/$dir" ]; then
            echo "  Cleaning $dir..."
            pushd "$EXTERNAL_SRC_DIR/$dir" > /dev/null
            rm -rf build_android cross_file.txt
            # 清理 Meson 子项目克隆，强制使用 pkg-config
            if [ -d subprojects ]; then
                find subprojects -maxdepth 1 -type d ! -name "subprojects" -exec rm -rf {} +
            fi
            if [ -f Makefile ] || [ -f makefile ]; then
                make distclean > /dev/null 2>&1 || make clean > /dev/null 2>&1 || true
            fi
            if [ "$dir" == "ffmpeg" ]; then
                rm -f config.h config.log config.mak ffbuild/config.mak ffbuild/config.sh ffbuild/config.log
            fi
            if [ "$dir" == "dav1d" ]; then
                rm -rf subprojects/.wraplock
            fi
            rm -rf meson-dist meson-logs meson-private
            popd > /dev/null
        fi
    done
}

# --- 检查帮助或全局清理 ---
if [[ "$1" == "-h" || "$1" == "--help" ]]; then
    show_help
    exit 0
fi

if [[ "$1" == "clean" ]]; then
    CLEAN_SRC=false
    CLEAN_SO=false
    shift
    if [[ $# -eq 0 ]]; then
        CLEAN_SRC=true
        CLEAN_SO=true
    else
        while [[ $# -gt 0 ]]; do
            case $1 in
                --src) CLEAN_SRC=true ;;
                --so) CLEAN_SO=true ;;
                *) echo "Unknown clean option: $1"; show_help; exit 1 ;;
            esac
            shift
        done
    fi
    [ "$CLEAN_SRC" = true ] && clean_src "all"
    if [ "$CLEAN_SO" = true ]; then
        echo "--- Removing all prebuilt libraries in $PREBUILT_DIR ---"
        # shellcheck disable=SC2115
        rm -rf "$PREBUILT_DIR"/*
    fi
    echo "Cleanup complete."
    exit 0
fi

if [[ "$1" == "libraries" || "$1" == "versions" ]]; then
    generate_libraries_json
    exit 0
fi

# --- 智能参数解析与标准化 ---
normalize_arch() {
    case "$1" in
        arm64|arm64-v8a) echo "arm64" ;;
        armv7l|armeabi-v7a|armv7|armv7a) echo "armv7l" ;;
        x86) echo "x86" ;;
        x86_64) echo "x86_64" ;;
        32|64|all) echo "$1" ;;
        *) echo "" ;;
    esac
}

is_valid_component() {
    case "$1" in
        mbedtls|lua|dav1d|libxml2|freetype2|fribidi|harfbuzz|unibreak|fontconfig|libass|curl|libwebp|ffmpeg|libplacebo|mpv|shaderc|all) return 0 ;;
        *) return 1 ;;
    esac
}

RAW_ARG1=$1
RAW_ARG2=$2

ARCH_ARG=""
ACTION=""

NORM_ARG1=$(normalize_arch "$RAW_ARG1")

if [ -z "$RAW_ARG1" ]; then
    # 未传任何参数：默认 arm64 all
    ARCH_ARG="arm64"
    ACTION="all"
elif [ -n "$NORM_ARG1" ]; then
    # $1 是有效的架构名（如 arm64, armv7l, 64, all 等）
    ARCH_ARG="$NORM_ARG1"
    if [ -z "$RAW_ARG2" ]; then
        ACTION="all"
    elif is_valid_component "$RAW_ARG2"; then
        ACTION="$RAW_ARG2"
    else
        echo "Error: Unknown build action '$RAW_ARG2'"
        show_help
        exit 1
    fi
elif is_valid_component "$RAW_ARG1"; then
    # $1 是有效组件名（如 ffmpeg, mpv, lua 等）
    ACTION="$RAW_ARG1"
    NORM_ARG2=$(normalize_arch "$RAW_ARG2")
    if [ -z "$RAW_ARG2" ]; then
        ARCH_ARG="arm64"
    elif [ -n "$NORM_ARG2" ]; then
        ARCH_ARG="$NORM_ARG2"
    else
        echo "Error: Unknown architecture '$RAW_ARG2'"
        show_help
        exit 1
    fi
else
    echo "Error: Unknown architecture or build action '$RAW_ARG1'"
    show_help
    exit 1
fi

# --- 执行构建任务 ---
perform_action() {
    case $ACTION in
        mbedtls|lua|dav1d|libxml2|freetype2|fribidi|harfbuzz|unibreak|fontconfig|libass|curl|libwebp|ffmpeg|libplacebo|mpv|shaderc)
            build_"$ACTION" ;;
        all)
            build_mbedtls
            build_lua
            build_dav1d
            build_libxml2
            build_freetype2
            build_fribidi
            build_harfbuzz
            build_unibreak
            build_fontconfig
            build_libass
            build_curl
            build_libwebp
            build_shaderc
            build_ffmpeg
            build_libplacebo
            build_mpv
            ;;
        *)
            echo "Unknown build action: $ACTION"
            exit 1 ;;
    esac
}

build_for_arch() {
    local target_arch=$1
    echo ""
    echo "##########################################################"
    echo "  STARTING BUILD FOR: $target_arch"
    echo "##########################################################"
    (
        source "$(dirname "${BASH_SOURCE[0]}")/env.sh" "$target_arch"
        export DEPS_PREFIX="$PREBUILT_DIR/$ABI"
        mkdir -p "$DEPS_PREFIX"
        clean_src "$ACTION"
        generate_vulkan_pc
        perform_action
    )
    echo "##########################################################"
    echo "  FINISHED BUILD FOR: $target_arch"
    echo "##########################################################"
}

# 架构判定与循环
case $ARCH_ARG in
    all) ARCHS=("arm64" "armv7l" "x86" "x86_64") ;;
    32)  ARCHS=("armv7l" "x86") ;;
    64)  ARCHS=("arm64" "x86_64") ;;
    arm64|armv7l|x86|x86_64) ARCHS=("$ARCH_ARG") ;;
    *) echo "Error: Unknown architecture group '$ARCH_ARG'"; exit 1 ;;
esac

for arch in "${ARCHS[@]}"; do
    build_for_arch "$arch"
done

generate_libraries_json
echo "Done! All tasks completed."
