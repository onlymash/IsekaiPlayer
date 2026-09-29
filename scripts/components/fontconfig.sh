#!/bin/bash

build_fontconfig() {
    echo "--- Building fontconfig ---"
    pushd "$EXTERNAL_SRC_DIR/fontconfig" > /dev/null
    rm -rf build_android && generate_meson_cross
    meson setup build_android --cross-file cross_file.txt --prefix="$DEPS_PREFIX" \
        --libdir=lib --default-library=static \
        -Dtests=disabled -Ddoc=disabled -Dtools=disabled -Dnls=disabled -Dxml-backend=libxml2
    ninja -C build_android install
    popd > /dev/null
}
