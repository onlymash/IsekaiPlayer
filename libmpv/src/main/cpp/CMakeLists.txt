cmake_minimum_required(VERSION 3.22.1)
project("mpv-android-lib")

# --- 定义路径 ---
set(PREBUILT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../external/prebuilt/${ANDROID_ABI}")

# --- 声明预编译共享库 (FFmpeg & mpv) ---
macro(add_prebuilt_shared name filename)
    add_library(${name} SHARED IMPORTED)
    set_target_properties(${name} PROPERTIES
            IMPORTED_LOCATION "${PREBUILT_ROOT}/lib/${filename}"
    )
endmacro()

# 声明 FFmpeg 各个模块 (共享库)
add_prebuilt_shared(avcodec libavcodec.so)
add_prebuilt_shared(avfilter libavfilter.so)
add_prebuilt_shared(avformat libavformat.so)
add_prebuilt_shared(avutil libavutil.so)
add_prebuilt_shared(swresample libswresample.so)
add_prebuilt_shared(swscale libswscale.so)

# 声明 mpv 核心
add_prebuilt_shared(mpv libmpv.so)

# --- 导入头文件 ---
include_directories("${PREBUILT_ROOT}/include")

# --- 定义 JNI 胶水库 ---
add_library(mpv-android-jni SHARED
        mpv_jni.cpp
)

# --- 链接 ---
# 链接必要的共享库
target_link_libraries(mpv-android-jni
        mpv
        avcodec
        avfilter
        avformat
        avutil
        swresample
        swscale
        log
        android
        jnigraphics
)
