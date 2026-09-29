#!/bin/bash

build_curl() {
    echo "--- Building curl ---"
    pushd "$EXTERNAL_SRC_DIR/curl" > /dev/null
    [ -f configure ] || autoreconf -fi
    rm -rf build_android && mkdir build_android && cd build_android
    ../configure --host="$TARGET_TRIPLE" --with-mbedtls="$DEPS_PREFIX" --without-libpsl \
        --disable-shared --enable-static --disable-debug --prefix="$DEPS_PREFIX" \
        --disable-manual --disable-docs --disable-ares --disable-unix-sockets --disable-tls-srp --disable-doh \
        --disable-rtsp --disable-dict --disable-telnet --disable-tftp --disable-pop3 --disable-imap --disable-smb \
        --disable-smtp --disable-gopher --disable-mqtt --disable-ntlm -disable-hsts
    make -j"$NPROC" install
    popd > /dev/null
}
