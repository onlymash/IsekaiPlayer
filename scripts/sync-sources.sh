#!/bin/bash

set -e

# 加载版本号与仓库定义
source "$(dirname "$0")/versions.sh"

PROJECT_ROOT=$(cd "$(dirname "$0")/.." && pwd)
EXTERNAL_DIR="$PROJECT_ROOT/external/sources"
mkdir -p "$EXTERNAL_DIR"

DOWNLOAD_ONLY=0
TARGET_REPO="all"

show_help() {
    echo "Source Code Synchronizer & Downloader"
    echo ""
    echo "Usage:"
    echo "  $0 [options] [target]"
    echo ""
    echo "Targets:"
    echo "  all                  - Process all native dependencies (default)"
    echo "  <component>          - Process a specific component (e.g., ffmpeg, mpv, libass...)"
    echo ""
    echo "Options:"
    echo "  -d, --download-only, --fast"
    echo "                       - Fast mode: only clone missing repos, skip existing ones"
    echo "  -h, --help           - Show this help message"
    echo ""
    echo "Examples:"
    echo "  $0                   # Sync/update all repositories"
    echo "  $0 ffmpeg            # Sync/update only ffmpeg"
    echo "  $0 -d                # Download missing repositories only"
    echo "  $0 --fast mpv        # Download mpv if missing, skip if existing"
}

# 参数解析
while [[ $# -gt 0 ]]; do
    case "$1" in
        -d|--download-only|--fast)
            DOWNLOAD_ONLY=1
            shift
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        -*)
            echo "Error: Unknown option $1" >&2
            show_help
            exit 1
            ;;
        *)
            TARGET_REPO="$1"
            shift
            ;;
    esac
done

MATCHED_COUNT=0

process_repo() {
    local url=$1
    local dir=$2
    local target_ref=$3
    local recursive=$4

    if [ "$TARGET_REPO" != "all" ] && [ "$TARGET_REPO" != "$dir" ]; then
        return
    fi

    MATCHED_COUNT=$((MATCHED_COUNT + 1))
    local repo_dir="$EXTERNAL_DIR/$dir"

    echo ">>> Processing $dir ($target_ref)..."

    if [ ! -d "$repo_dir" ]; then
        echo "    Directory missing, performing fresh clone..."
        local args=("--depth" "1" "-b" "$target_ref")
        [ "$recursive" == "1" ] && args+=("--recursive")
        git clone "${args[@]}" "$url" "$repo_dir"
        return
    fi

    # 处于 --download-only / --fast 模式且目录已存在
    if [ "$DOWNLOAD_ONLY" == "1" ]; then
        if [ ! -d "$repo_dir/.git" ]; then
            echo "    Warning: $dir exists but is not a Git repository. Skipping..." >&2
            return
        fi

        if [ "$recursive" == "1" ]; then
            echo "    Checking submodules for $dir..."
            git -C "$repo_dir" submodule update --init --recursive
        fi
        echo "    Already exists, skipping update (fast mode)."
        return
    fi

    pushd "$repo_dir" > /dev/null

    # 检查当前是否在 Git 控制下
    if [ ! -d ".git" ]; then
        echo "    Warning: $dir is not a Git repository. Skipping update."
        popd > /dev/null
        return
    fi

    # 重置本地修改以确保更新/切换成功 (处理 build-native.sh 中打的补丁)
    git reset --hard HEAD > /dev/null
    git clean -df > /dev/null

    # 获取当前所在的分支或 Tag
    local current_ref
    current_ref=$(git symbolic-ref --short HEAD 2>/dev/null || git describe --tags --exact-match 2>/dev/null || echo "detached")

    if [ "$target_ref" == "master" ] || [ "$target_ref" == "main" ]; then
        echo "    Target is $target_ref. Performing pull..."
        git checkout "$target_ref" 2>/dev/null || git checkout -b "$target_ref" "origin/$target_ref"
        git pull origin "$target_ref"
    elif [ "$current_ref" != "$target_ref" ]; then
        echo "    Switching ref: $current_ref -> $target_ref"
        git fetch origin "$target_ref" --depth 1
        git checkout "$target_ref" 2>/dev/null || git checkout -B "$target_ref" FETCH_HEAD
    else
        echo "    Already at $target_ref. No ref change needed."
    fi

    # 处理子模块
    if [ "$recursive" == "1" ]; then
        echo "    Updating submodules..."
        git submodule update --init --recursive
    fi

    popd > /dev/null
}

for_each_repo process_repo

if [ "$TARGET_REPO" != "all" ] && [ "$MATCHED_COUNT" -eq 0 ]; then
    echo "Error: Component '$TARGET_REPO' not found in versions.sh." >&2
    exit 1
fi

echo "All specified source repositories processed successfully."
