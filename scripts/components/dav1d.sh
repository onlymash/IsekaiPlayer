#!/bin/bash

build_dav1d() {
    echo "--- Building dav1d ---"
    pushd "$EXTERNAL_SRC_DIR/dav1d" > /dev/null
    rm -rf build_android && generate_meson_cross
    meson setup build_android --cross-file cross_file.txt --prefix="$DEPS_PREFIX" \
        --libdir=lib --default-library=static -Dlogging=false \
        -Denable_tools=false -Denable_tests=false -Denable_examples=false \
        -Db_lto=true -Dstack_alignment=16
    ninja -C build_android install
    popd > /dev/null
}
