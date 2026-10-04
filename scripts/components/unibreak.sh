#!/bin/bash

build_unibreak() {
    echo "--- Building unibreak ---"
    pushd "$EXTERNAL_SRC_DIR/unibreak" > /dev/null
    autoreconf -fi
    rm -rf build_android && mkdir build_android && cd build_android
    ../configure --host="$TARGET_TRIPLE" --with-pic --enable-static --disable-shared --prefix="$DEPS_PREFIX"
    make -j"$NPROC" install
    popd > /dev/null
}
