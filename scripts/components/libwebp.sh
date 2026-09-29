#!/bin/bash

build_libwebp() {
    echo "--- Building libwebp ---"
    pushd "$EXTERNAL_SRC_DIR/libwebp" > /dev/null
    rm -rf build_android && mkdir build_android && cd build_android
    cmake .. -DCMAKE_TOOLCHAIN_FILE="$NDK_PATH/build/cmake/android.toolchain.cmake" \
        -DANDROID_ABI="$ABI" -DANDROID_PLATFORM=android-$API_LEVEL \
        -DCMAKE_BUILD_TYPE=Release \
        -DBUILD_SHARED_LIBS=OFF \
        -DWEBP_BUILD_ANIM_UTILS=OFF \
        -DWEBP_BUILD_CWEBP=OFF \
        -DWEBP_BUILD_DWEBP=OFF \
        -DWEBP_BUILD_GIF2WEBP=OFF \
        -DWEBP_BUILD_IMG2WEBP=OFF \
        -DWEBP_BUILD_VWEBP=OFF \
        -DWEBP_BUILD_WEBPINFO=OFF \
        -DWEBP_BUILD_WEBPMUX=OFF \
        -DWEBP_BUILD_EXTRAS=OFF \
        -DCMAKE_INSTALL_PREFIX="$DEPS_PREFIX"
    make -j"$NPROC" install
    if [ -f "$DEPS_PREFIX/lib/pkgconfig/libwebp.pc" ]; then
        sed -i 's/Libs: -L\${libdir} -lwebp/Libs: -L\${libdir} -lwebp -lsharpyuv/' "$DEPS_PREFIX/lib/pkgconfig/libwebp.pc"
    fi
    popd > /dev/null
}
