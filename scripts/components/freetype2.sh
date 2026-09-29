#!/bin/bash

build_freetype2() {
    echo "--- Building freetype2 ---"
    pushd "$EXTERNAL_SRC_DIR/freetype2" > /dev/null
    rm -rf build_android && generate_meson_cross
    meson setup build_android --cross-file cross_file.txt --prefix="$DEPS_PREFIX" \
        --libdir=lib --default-library=static \
        -Dzlib=disabled -Dbrotli=disabled -Dpng=disabled -Dharfbuzz=disabled -Dbzip2=disabled
    ninja -C build_android install
    popd > /dev/null
}
