#!/bin/bash

build_libass() {
    echo "--- Building libass ---"
    pushd "$EXTERNAL_SRC_DIR/libass" > /dev/null
    autoreconf -fi
    rm -rf build_android && mkdir build_android && cd build_android
    ../configure --host="$TARGET_TRIPLE" --with-pic --enable-static --disable-shared \
        --enable-libunibreak --enable-fontconfig --prefix="$DEPS_PREFIX"
    make -j"$NPROC" install
    popd > /dev/null
}
