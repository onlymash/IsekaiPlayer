#!/bin/bash

build_ffmpeg() {
    echo "--- Building FFmpeg ---"
    pushd "$EXTERNAL_SRC_DIR/ffmpeg" > /dev/null
    rm -rf ffbuild/config.mak

    local extra_conf=""
    # 特殊处理 x86：禁用可能导致文本重定位问题的汇编优化
    if [ "$ABI" == "x86" ]; then
      echo "  Applying x86 specific fixes..."
      extra_conf="--disable-asm"
    fi

    # 显式透传全局 CFLAGS 和 LDFLAGS 确保环境一致性
    ./configure \
        --target-os=android --arch="$FF_ARCH" --cpu="$FF_CPU" --enable-cross-compile \
        --prefix="$DEPS_PREFIX" --cc="$CC" --cxx="$CXX" --ar="$AR" --strip="$STRIP" --nm="$NM" --ranlib="$RANLIB" \
        --pkg-config="pkg-config" \
        --enable-shared --disable-static \
        --enable-{jni,mediacodec,mbedtls,hwaccels} \
        --enable-{libdav1d,libxml2,version3} \
        --disable-{stripping,doc,programs,symver,vulkan,gpl} \
        --disable-{muxers,encoders,devices,avdevice} \
        --enable-libwebp \
        --enable-encoder=mjpeg,png,libwebp \
        --enable-muxer=mov,matroska,mpegts,webp,image2 \
        --extra-cflags="$CFLAGS -I$DEPS_PREFIX/include" \
        --extra-ldflags="$LDFLAGS -L$DEPS_PREFIX/lib" \
        --extra-libs="-lsharpyuv" \
        $extra_conf \
        || {
            echo "FFmpeg configure failed! Check ffbuild/config.log for details."
            exit 1
        }

    make -j"$NPROC" install
    popd > /dev/null
}
