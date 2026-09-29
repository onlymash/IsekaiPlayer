cmake_minimum_required(VERSION 3.22.1)
project("media-ffmpeg-jni-lib")

set(PREBUILT_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../../external/prebuilt/${ANDROID_ABI}")

macro(add_prebuilt_shared name filename)
    add_library(${name} SHARED IMPORTED)
    set_target_properties(${name} PROPERTIES
            IMPORTED_LOCATION "${PREBUILT_ROOT}/lib/${filename}"
    )
endmacro()

add_prebuilt_shared(avcodec libavcodec.so)
add_prebuilt_shared(avutil libavutil.so)
add_prebuilt_shared(swresample libswresample.so)

include_directories("${PREBUILT_ROOT}/include")

add_library(media-ffmpeg-jni SHARED
        media_ffmpeg_jni.cc
)

target_link_libraries(media-ffmpeg-jni
        avcodec
        avutil
        swresample
        log
)
