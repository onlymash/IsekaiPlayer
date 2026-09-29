#!/bin/bash
# scripts/utils.sh

# --- 生成 Meson cross file ---
generate_meson_cross() {
    local cpu_fam=""
    case $ABI in
        arm64-v8a)  cpu_fam="aarch64" ;;
        armeabi-v7l) cpu_fam="arm" ;;
        armeabi-v7a) cpu_fam="arm" ;;
        x86_64)      cpu_fam="x86_64" ;;
        x86)         cpu_fam="x86" ;;
    esac

    # 将 CFLAGS/CXXFLAGS 转换为 Meson 列表格式 ['arg1', 'arg2', ...]
    local c_args_meson=""
    for arg in $CFLAGS; do
        c_args_meson+="'$arg', "
    done
    local cpp_args_meson=""
    for arg in $CXXFLAGS; do
        cpp_args_meson+="'$arg', "
    done

    cat > cross_file.txt <<EOF
[binaries]
c = '$CC'
cpp = '$CXX'
ar = '$AR'
nm = '$NM'
strip = '$STRIP'
pkg-config = 'pkg-config'
pkgconfig = 'pkg-config'

[built-in options]
buildtype = 'release'
default_library = 'static'
c_args = [${c_args_meson%, }]
cpp_args = [${cpp_args_meson%, }]

[properties]
pkg_config_libdir = '$PREBUILT_DIR/$ABI/lib/pkgconfig'

[host_machine]
system = 'android'
cpu_family = '$cpu_fam'
cpu = '$FF_ARCH'
endian = 'little'
EOF
}

# --- 生成 vulkan.pc ---
generate_vulkan_pc() {
    echo "--- Generating vulkan.pc ---"
    mkdir -p "$DEPS_PREFIX/lib/pkgconfig"
    cat > "$DEPS_PREFIX/lib/pkgconfig/vulkan.pc" <<EOF
Name: Vulkan
Description: Vulkan Loader
Version: 1.3.284
Libs: -lvulkan
Cflags:
EOF
}

# --- 生成库信息 JSON (libraries.json) 供 App 使用 ---
generate_libraries_json() {
    echo "--- Generating libraries.json ---"
    local output_dir="$EXTERNAL_DIR/prebuilt/assets"
    mkdir -p "$output_dir"
    local output_file="$output_dir/libraries.json"

    local versions_sh="$PROJECT_ROOT/scripts/versions.sh"
    if [ ! -f "$versions_sh" ]; then
        echo "Warning: $versions_sh not found, skipping JSON generation."
        return
    fi

    # shellcheck source=/dev/null
    source "$versions_sh"

    local tmp_json="/tmp/gateplay_libraries.json"
    echo "[" > "$tmp_json"

    local first=true

    write_json_entry() {
        local name=$1
        local version=$2
        local description=$3
        local project_url=$4
        local license_url=$5
        local license_name=$6

        if [ "$first" = true ]; then
            first=false
        else
            echo "," >> "$tmp_json"
        fi

        printf '  {\n    "name": "%s",\n    "version": "%s",\n    "description": "%s",\n    "project_url": "%s",\n    "license_url": "%s",\n    "license_name": "%s"\n  }' \
            "$name" "$version" "$description" "$project_url" "$license_url" "$license_name" >> "$tmp_json"
    }

    process_lib() {
        local _git_url=$1
        local dir=$2
        local target_ref=$3
        local _recursive=$4
        local name=$5
        local project_url=$6
        local license_name=$7
        local license_url=$8
        local description=$9

        local final_val="$target_ref"
        local src_path="$EXTERNAL_SRC_DIR/$dir"

        if [[ "$target_ref" == "master" || "$target_ref" == "main" ]]; then
            if [ -d "$src_path/.git" ]; then
                local git_commit
                git_commit=$(git -C "$src_path" rev-parse --short HEAD 2>/dev/null)
                if [ -n "$git_commit" ]; then
                    final_val="$git_commit"
                fi
            fi
        fi

        # 简单清理：去掉开头的 'v' 或 '库名-'，并将 _ 和 - 替换为 .
        local clean_val
        clean_val=$(echo "$final_val" | sed 's/^v//; s/^[a-zA-Z0-9]*[-_]//; s/[_-]/./g')

        write_json_entry "$name" "$clean_val" "$description" "$project_url" "$license_url" "$license_name"
    }

    process_app_lib() {
        local name=$1
        local version=$2
        local project_url=$3
        local license_name=$4
        local license_url=$5
        local description=$6

        write_json_entry "$name" "$version" "$description" "$project_url" "$license_url" "$license_name"
    }

    for_each_repo process_lib
    for_each_app_repo process_app_lib

    echo "" >> "$tmp_json"
    echo "]" >> "$tmp_json"

    cp "$tmp_json" "$output_file"
    rm -f "$tmp_json"

    echo "Generated: $output_file"
}
