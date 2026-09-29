#!/bin/bash

build_libplacebo() {
    echo "--- Building libplacebo ---"
    pushd "$EXTERNAL_SRC_DIR/libplacebo" > /dev/null
    rm -rf build_android && generate_meson_cross
    meson setup build_android --cross-file cross_file.txt --prefix="$DEPS_PREFIX" \
        --libdir=lib --default-library=static \
        -Dshaderc=enabled -Dvk-proc-addr=enabled -Dlcms=disabled \
        -Ddemos=false -Dtests=false -Dbench=false
    ninja -C build_android install
    sed -i.bak '/^Libs:/ s|$| -lc++|' "$DEPS_PREFIX/lib/pkgconfig/libplacebo.pc" && rm -f "$DEPS_PREFIX/lib/pkgconfig/libplacebo.pc.bak"
    popd > /dev/null
}
