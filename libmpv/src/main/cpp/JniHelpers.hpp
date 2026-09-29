#pragma once

#include <jni.h>
#include <string>
#include <vector>

/**
 * JniHelpers 是一个性能优化助手类。
 * 它在应用启动时一次性缓存常用的 Java 类引用、方法 ID 和字段 ID。
 * 这样做可以避免在频繁调用的事件循环（如进度更新）中重复进行昂贵的 JNI 反射查找。
 */
class JniHelpers {
public:
    /**
     * 初始化函数：在第一个 MpvLib 实例创建时调用。
     * 使用 GlobalRef 确保类引用在整个应用生命周期内有效。
     */
    static void init(JNIEnv *env) {
        if (cached) return;

        // 缓存 MpvLib 类及其事件回调方法
        class_MPVLib = (jclass) env->NewGlobalRef(env->FindClass("com/fiepi/mpv/MpvLib"));

        // 对应 Kotlin 中的 eventProperty 重载方法（不同数据类型）
        method_eventProperty_S = env->GetMethodID(class_MPVLib, "eventProperty", "(Ljava/lang/String;)V");
        method_eventProperty_Sb = env->GetMethodID(class_MPVLib, "eventProperty", "(Ljava/lang/String;Z)V");
        method_eventProperty_Sl = env->GetMethodID(class_MPVLib, "eventProperty", "(Ljava/lang/String;J)V");
        method_eventProperty_Sd = env->GetMethodID(class_MPVLib, "eventProperty", "(Ljava/lang/String;D)V");
        method_eventProperty_SS = env->GetMethodID(class_MPVLib, "eventProperty", "(Ljava/lang/String;Ljava/lang/String;)V");

        // 对应 Kotlin 中的通用 event(eventId: Int) 方法
        method_event = env->GetMethodID(class_MPVLib, "event", "(I)V");

        // 对应 Kotlin 中的 logMessage(prefix: String, level: Int, text: String) 方法
        method_logMessage = env->GetMethodID(class_MPVLib, "logMessage", "(Ljava/lang/String;ILjava/lang/String;)V");

        // 缓存常用的包装类，用于 getProperty 时将原生类型转换为对象
        class_Integer = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Integer"));
        method_Integer_init = env->GetMethodID(class_Integer, "<init>", "(I)V");

        class_Double = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Double"));
        method_Double_init = env->GetMethodID(class_Double, "<init>", "(D)V");

        class_Boolean = (jclass) env->NewGlobalRef(env->FindClass("java/lang/Boolean"));
        method_Boolean_init = env->GetMethodID(class_Boolean, "<init>", "(Z)V");

        // 缓存 Bitmap 相关类，用于 grabThumbnail 截图功能
        class_Bitmap = (jclass) env->NewGlobalRef(env->FindClass("android/graphics/Bitmap"));
        class_BitmapConfig = (jclass) env->NewGlobalRef(env->FindClass("android/graphics/Bitmap$Config"));
        method_Bitmap_create = env->GetStaticMethodID(class_Bitmap, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
        field_ARGB_8888 = env->GetStaticFieldID(class_BitmapConfig, "ARGB_8888", "Landroid/graphics/Bitmap$Config;");

        cached = true;
    }

    // --- 静态成员变量 ---
    static inline bool cached = false;

    // 全局类引用
    static inline jclass class_MPVLib, class_Integer, class_Double, class_Boolean, class_Bitmap, class_BitmapConfig;

    // 方法 ID
    static inline jmethodID method_eventProperty_S, method_eventProperty_Sb, method_eventProperty_Sl,
            method_eventProperty_Sd, method_eventProperty_SS, method_event, method_logMessage,
            method_Integer_init, method_Double_init, method_Boolean_init, method_Bitmap_create;

    // 字段 ID
    static inline jfieldID field_ARGB_8888;
};
