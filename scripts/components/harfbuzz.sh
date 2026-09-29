#!/bin/bash

build_harfbuzz() {
    echo "--- Building harfbuzz ---"
    pushd "$EXTERNAL_SRC_DIR/harfbuzz" > /dev/null
    rm -rf build_android && generate_meson_cross
    meson setup build_android --cross-file cross_file.txt --prefix="$DEPS_PREFIX" \
        --libdir=lib --default-library=static \
        -Dtests=disabled -Ddocs=disabled \
        -D{raster,vector,gpu,subset,glib,icu}=disabled
    ninja -C build_android install
    popd > /dev/null
}
