#!/bin/bash

build_libxml2() {
    echo "--- Building libxml2 ---"
    pushd "$EXTERNAL_SRC_DIR/libxml2" > /dev/null
    rm -rf build_android && generate_meson_cross
    meson setup build_android --cross-file cross_file.txt --prefix="$DEPS_PREFIX" \
        --libdir=lib --default-library=static \
        -Dminimum=true -Dpush=enabled -Dreader=enabled -Dsax1=enabled \
        -Diso8859x=enabled -Dpattern=enabled -Diconv=disabled -Dhttp=disabled \
        -Dpython=disabled -Dzlib=disabled
    ninja -C build_android install
    popd > /dev/null
}
