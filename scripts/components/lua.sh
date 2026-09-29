#!/bin/bash

build_lua() {
    echo "--- Building lua ---"
    pushd "$EXTERNAL_SRC_DIR/lua/src" > /dev/null
    make clean || true
    local mycflags="-fPIC -Dgetlocaledecpoint\\(\\)=\\(46\\) -Dlua_fseek=fseek"
    make CC="$CC" AR="$AR rcu" RANLIB="$RANLIB" MYCFLAGS="$mycflags" PLAT=linux LUA_T= LUAC_T= -j"$NPROC"
    mkdir -p "$DEPS_PREFIX/include" "$DEPS_PREFIX/lib/pkgconfig"
    cp lua.h luaconf.h lauxlib.h lualib.h "$DEPS_PREFIX/include/"
    cp liblua.a "$DEPS_PREFIX/lib/"
    cat > "$DEPS_PREFIX/lib/pkgconfig/lua.pc" <<EOF
prefix=$DEPS_PREFIX
exec_prefix=\${prefix}
libdir=\${exec_prefix}/lib
includedir=\${prefix}/include

Name: Lua
Description: Lua language engine
Version: 5.2.4
Libs: -L\${libdir} -llua -lm
Cflags: -I\${includedir}
EOF
    popd > /dev/null
}
