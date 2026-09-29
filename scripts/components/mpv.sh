#!/bin/bash

build_mpv() {
    echo "--- Building mpv ---"
    pushd "$EXTERNAL_SRC_DIR/mpv" > /dev/null

    local patched=false
    # 尝试应用针对 Android 优化的着色器补丁
    local patch_file="$PROJECT_ROOT/external/patches/mpv-shaders.patch"
    if [ -f "$patch_file" ]; then
        echo "  Applying mpv-shaders.patch..."
        if patch -p1 -N --dry-run < "$patch_file" >/dev/null 2>&1; then
            patch -p1 < "$patch_file"
            patched=true
        else
            echo "  Patch detection skipped (likely already applied)."
        fi
    fi

    rm -rf build_android && generate_meson_cross
    export PKG_CONFIG_PATH="$DEPS_PREFIX/lib/pkgconfig"
    meson setup build_android --cross-file cross_file.txt --prefix="$DEPS_PREFIX" \
        --libdir=lib --default-library=shared \
        -Dgpl=false -Diconv=disabled -Dlua=enabled -Dvulkan=enabled \
        -Dlibmpv=true -Dcplayer=false \
        -Dmanpage-build=disabled -Dlcms2=disabled
    ninja -C build_android install
    echo "--- Stripping symbols ---"
    "$STRIP" --strip-unneeded "$DEPS_PREFIX/lib/"*.so

    # 编译完成后撤销补丁，保持源码树整洁，方便下次 git pull 顺利执行
    if [ "$patched" = true ]; then
        echo "  Reverting mpv-shaders.patch..."
        patch -p1 -R < "$patch_file"
    fi

    popd > /dev/null
}
