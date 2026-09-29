#!/bin/bash

build_shaderc() {
    echo "--- Building shaderc ---"
    local shaderc_build_dir="$EXTERNAL_SRC_DIR/shaderc_build"
    rm -rf "$shaderc_build_dir" && mkdir -p "$shaderc_build_dir"
    pushd "$shaderc_build_dir" > /dev/null

    "$NDK_PATH/ndk-build" -C "$NDK_PATH/sources/third_party/shaderc" -j"$NPROC" \
        NDK_PROJECT_PATH=. \
        APP_BUILD_SCRIPT=Android.mk \
        APP_ABI="$ABI" \
        APP_PLATFORM=android-"$API_LEVEL" \
        APP_STL=c++_static \
        NDK_APP_OUT="$shaderc_build_dir" \
        NDK_APP_LIBS_OUT="$shaderc_build_dir/libs" \
        libshaderc_combined

    mkdir -p "$DEPS_PREFIX/include" "$DEPS_PREFIX/lib/pkgconfig"
    cp -r "$NDK_PATH/sources/third_party/shaderc/libshaderc/include/shaderc" "$DEPS_PREFIX/include/"

    # 优先拷贝真正的合并库 libshaderc_combined.a
    if [ -f "local/$ABI/libshaderc_combined.a" ]; then
        cp "local/$ABI/libshaderc_combined.a" "$DEPS_PREFIX/lib/libshaderc_combined.a"
    elif [ -f "local/$ABI/libshaderc.a" ]; then
        # 如果只有 libshaderc.a 且它实际上是合并后的（某些 NDK 版本行为），则拷贝它
        cp "local/$ABI/libshaderc.a" "$DEPS_PREFIX/lib/libshaderc_combined.a"
    fi

    cat > "$DEPS_PREFIX/lib/pkgconfig/shaderc.pc" <<EOF
prefix=$DEPS_PREFIX
exec_prefix=\${prefix}
libdir=\${exec_prefix}/lib
includedir=\${prefix}/include

Name: shaderc
Description: Shaderc combined library
Version: 2023.0
Libs: -L\${libdir} -lshaderc_combined
Cflags: -I\${includedir}
EOF
    popd > /dev/null
}
