#!/bin/bash

build_mbedtls() {
    echo "--- Building mbedtls ---"
    pushd "$EXTERNAL_SRC_DIR/mbedtls" > /dev/null

    # mbedtls 3.6.6+ starts using /dev/urandom, set MBEDTLS_PLATFORM_DEV_RANDOM to /dev/urandom
    python3 scripts/config.py set MBEDTLS_PLATFORM_DEV_RANDOM '"/dev/urandom"'

    rm -rf build_android && mkdir build_android && cd build_android
    cmake .. -DCMAKE_TOOLCHAIN_FILE="$NDK_PATH/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$ABI" -DANDROID_PLATFORM=android-$API_LEVEL \
        -DCMAKE_BUILD_TYPE=Release -DENABLE_TESTING=OFF -DENABLE_PROGRAMS=OFF \
        -DUSE_SHARED_MBEDTLS_LIBRARY=OFF -DUSE_STATIC_MBEDTLS_LIBRARY=ON \
        -DCMAKE_INSTALL_PREFIX="$DEPS_PREFIX"
    make -j"$NPROC" install
    popd > /dev/null
}
