#!/bin/bash

build_fribidi() {
    echo "--- Building fribidi ---"
    pushd "$EXTERNAL_SRC_DIR/fribidi" > /dev/null
    rm -rf build_android && generate_meson_cross
    meson setup build_android --cross-file cross_file.txt --prefix="$DEPS_PREFIX" \
        --libdir=lib --default-library=static -D{tests,docs}=false
    ninja -C build_android install
    popd > /dev/null
}
