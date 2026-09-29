#include <jni.h>
#include <cstdlib>
#include <cstdio>
#include <ctime>
#include <clocale>
#include <atomic>
#include <vector>
#include <string>
#include <algorithm>
#include <cmath>
#include <cstring>
#include <pthread.h>
#include <android/native_window_jni.h>
#include <android/bitmap.h>

#include <mpv/client.h>
#include <mpv/render_gl.h>

#include "JniHelpers.hpp"
#include "MpvContext.hpp"

// FFmpeg 核心库
extern "C" {
#include <libavcodec/jni.h>
}

/**
 * 全局 JavaVM 指针。
 * JNIEnv 与线程绑定，不能跨线程使用。
 * 在 Native 子线程中，我们需要通过 JavaVM->AttachCurrentThread 获取该线程专属的 JNIEnv。
 */
static JavaVM *g_vm = nullptr;

/**
 * 助手函数：从 Java 层 MpvLib 对象的 nativeContext 字段中取出 C++ MpvContext 指针。
 * 每一个 MpvLib 实例在 Kotlin 层都持有一个 Long 类型的 nativeContext 变量，存储 Native 对象的内存地址。
 */
static MpvContext *get_ctx(JNIEnv *env, jobject obj) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "nativeContext", "J");
    return reinterpret_cast<MpvContext *>(env->GetLongField(obj, fid));
}

/**
 * 助手函数：将 C++ MpvContext 指针保存到 Java 层 MpvLib 对象的 nativeContext 字段中。
 */
static void set_ctx(JNIEnv *env, jobject obj, MpvContext *ctx) {
    jclass cls = env->GetObjectClass(obj);
    jfieldID fid = env->GetFieldID(cls, "nativeContext", "J");
    env->SetLongField(obj, fid, reinterpret_cast<jlong>(ctx));
}

// 快速宏定义：检查 Context 是否有效，防止在未初始化或已销毁状态下调用导致崩溃
#define CHECK_MPV_INIT_VOID(ctx) if (!ctx) return;
#define CHECK_MPV_INIT_RET(ctx, val) if (!ctx) return val;

/**
 * 将 mpv 属性变更事件分发回 Java 层。
 * 根据属性的格式（int, double, string 等），调用对应的 Java 重载方法。
 * 包含高频属性（如 time-pos, demuxer-cache-duration）的 C++ 节流逻辑。
 */
static void sendPropertyUpdateToJava(JNIEnv *env, MpvContext *ctx, mpv_event_property *prop) {
    if (prop->format == MPV_FORMAT_DOUBLE && prop->data) {
        if (strcmp(prop->name, "time-pos") == 0) {
            double current_pos = *(double *) prop->data;
            // 只有当进度变动 >= 0.5 秒（2Hz 节流），或者重置/seek（跳变 >= 0.5s）时才向下传递
            if (ctx && std::abs(current_pos - ctx->last_sent_time_pos) < 0.5) {
                return;
            }
            if (ctx) ctx->last_sent_time_pos = current_pos;
        } else if (strcmp(prop->name, "demuxer-cache-duration") == 0) {
            double current_cache = *(double *) prop->data;
            if (ctx && std::abs(current_cache - ctx->last_sent_demuxer_cache) < 0.5) {
                return;
            }
            if (ctx) ctx->last_sent_demuxer_cache = current_cache;
        }
    }

    jobject jInstance = ctx->getJInstance();
    jstring jprop = env->NewStringUTF(prop->name);
    switch (prop->format) {
        case MPV_FORMAT_NONE:
            env->CallVoidMethod(jInstance, JniHelpers::method_eventProperty_S, jprop);
            break;
        case MPV_FORMAT_FLAG:
            env->CallVoidMethod(jInstance, JniHelpers::method_eventProperty_Sb, jprop, (jboolean) (*(int *) prop->data != 0));
            break;
        case MPV_FORMAT_INT64:
            env->CallVoidMethod(jInstance, JniHelpers::method_eventProperty_Sl, jprop, (jlong) *(int64_t *) prop->data);
            break;
        case MPV_FORMAT_DOUBLE:
            env->CallVoidMethod(jInstance, JniHelpers::method_eventProperty_Sd, jprop, (jdouble) *(double *) prop->data);
            break;
        case MPV_FORMAT_STRING: {
            jstring jvalue = env->NewStringUTF(*(const char **) prop->data);
            env->CallVoidMethod(jInstance, JniHelpers::method_eventProperty_SS, jprop, jvalue);
            env->DeleteLocalRef(jvalue);
            break;
        }
        default:
            break;
    }
    env->DeleteLocalRef(jprop);
}

/**
 * 事件循环子线程函数。
 * 每一个 MpvContext 都会启动一个独立的 pthread。
 * 该线程阻塞在 mpv_wait_event 上，持续监听内核发出的事件并分发给 UI 层。
 */
void *event_thread_func(void *arg) {
    auto *ctx = static_cast<MpvContext *>(arg);
    JNIEnv *env = nullptr;

    // 将当前 Native 线程挂载到 JVM，获取 JNIEnv
    if (g_vm->AttachCurrentThread(&env, nullptr) != JNI_OK) return nullptr;

    jobject jInstance = ctx->getJInstance();

    while (!ctx->event_thread_request_exit) {
        // 阻塞等待事件，超时时间为 -1.0 (永久等待)
        mpv_event *mp_event = mpv_wait_event(ctx->getHandle(), -1.0);

        // 收到退出请求或内核关闭信号，退出循环
        if (ctx->event_thread_request_exit || mp_event->event_id == MPV_EVENT_SHUTDOWN) break;
        if (mp_event->event_id == MPV_EVENT_NONE) continue;

        switch (mp_event->event_id) {
            case MPV_EVENT_LOG_MESSAGE: {
                // 收到内核日志（如 [ffmpeg] 等），转发给 Kotlin 的 Logger
                auto msg = (mpv_event_log_message *) mp_event->data;
                jstring jprefix = env->NewStringUTF(msg->prefix);
                jstring jtext = env->NewStringUTF(msg->text);
                env->CallVoidMethod(jInstance, JniHelpers::method_logMessage, jprefix, (jint) msg->log_level, jtext);
                env->DeleteLocalRef(jprefix);
                env->DeleteLocalRef(jtext);
                break;
            }
            case MPV_EVENT_PROPERTY_CHANGE:
                // 收到观察的属性变更（如 time-pos, duration）
                sendPropertyUpdateToJava(env, ctx, (mpv_event_property *) mp_event->data);
                break;
            default:
                // 转发其他通用事件 ID（如播放结束、文件加载完成等）
                env->CallVoidMethod(jInstance, JniHelpers::method_event, (jint) mp_event->event_id);
                break;
        }
    }

    // 从 JVM 卸载当前线程
    g_vm->DetachCurrentThread();
    return nullptr;
}

extern "C" {

/**
 * 创建 MpvContext。
 * 负责全局 JavaVM 注册、FFmpeg 硬件解码环境初始化、以及单实例 Context 的内存分配。
 */
JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_create(JNIEnv *env, jobject obj, jobject appctx) {
    // 强制设置 locale 为 C，防止在某些语言环境下 double 转换解析出错（如 "," 代替 "."）
    setlocale(LC_NUMERIC, "C");

    if (!g_vm) {
        env->GetJavaVM(&g_vm);
        // 为 FFmpeg 传递 JavaVM 和 Context，这是 Android 硬件解码 MediaCodec 的前提
        av_jni_set_java_vm(g_vm, nullptr);
        av_jni_set_android_app_ctx(env->NewGlobalRef(appctx), nullptr);
        // 初始化全局 JNI 缓存
        JniHelpers::init(env);
    }

    MpvContext *ctx = get_ctx(env, obj);
    if (ctx) return; // 已创建，跳过

    // 创建 Native Context，并创建指向 Kotlin 实例的全局引用
    ctx = new MpvContext(env->NewGlobalRef(obj));
    set_ctx(env, obj, ctx);

    // 请求内核输出日志到回调
    mpv_request_log_messages(ctx->getHandle(), "terminal-default");
}

/**
 * 初始化 mpv 内核并启动事件线程。
 */
JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_init(JNIEnv *env, jobject obj) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_VOID(ctx);
    ctx->initialize();
    ctx->event_thread_request_exit = false;
    // 启动负责轮询内核事件的子线程
    pthread_create(&ctx->event_thread_id, nullptr, event_thread_func, ctx);
}

/**
 * 销毁 mpv 实例，释放所有 Native 与 Java 资源。
 */
JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_destroy(JNIEnv *env, jobject obj) {
    MpvContext *ctx = get_ctx(env, obj);
    if (!ctx) return;

    // 通知并等待事件线程退出
    ctx->event_thread_request_exit = true;
    mpv_wakeup(ctx->getHandle()); // 唤醒可能阻塞在 wait_event 上的线程
    pthread_join(ctx->event_thread_id, nullptr);

    // 释放全局引用
    if (ctx->g_surface) env->DeleteGlobalRef(ctx->g_surface);
    env->DeleteGlobalRef(ctx->getJInstance());

    // 销毁 Native 对象
    delete ctx;
    set_ctx(env, obj, nullptr);
}

/**
 * 绑定 Surface。
 * mpv 内部支持直接通过 Java Surface 对象作为 wid（Window ID）。
 */
JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_attachSurface(JNIEnv *env, jobject obj, jobject surface) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_VOID(ctx);
    if (ctx->g_surface) env->DeleteGlobalRef(ctx->g_surface);
    // 持有 Surface 的全局引用，防止其被 GC 回收
    ctx->g_surface = env->NewGlobalRef(surface);
    auto wid = (int64_t) (intptr_t) ctx->g_surface;
    // 将对象指针地址作为 "wid" 传递给内核，mpv 的 Android 后端会自动处理
    mpv_set_option(ctx->getHandle(), "wid", MPV_FORMAT_INT64, &wid);
}

/**
 * 解绑 Surface。
 */
JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_detachSurface(JNIEnv *env, jobject obj) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_VOID(ctx);
    int64_t wid = 0;
    // 设置 wid 为 0 即可断开 Surface 引用
    mpv_set_option(ctx->getHandle(), "wid", MPV_FORMAT_INT64, &wid);
    if (ctx->g_surface) {
        env->DeleteGlobalRef(ctx->g_surface);
        ctx->g_surface = nullptr;
    }
}

/**
 * 执行 mpv 命令行（类似 ./mpv --arg1 --arg2）。
 */
JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_nativeCommand(JNIEnv *env, jobject obj, jobjectArray jarray) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_VOID(ctx);
    jsize len = env->GetArrayLength(jarray);
    std::vector<std::string> str_args;
    for (jsize i = 0; i < len; ++i) {
        auto js = (jstring) env->GetObjectArrayElement(jarray, i);
        const char *s = env->GetStringUTFChars(js, nullptr);
        if (s) str_args.emplace_back(s);
        env->ReleaseStringUTFChars(js, s);
        env->DeleteLocalRef(js);
    }
    std::vector<const char *> args;
    args.reserve(str_args.size() + 1); // 额外预留一个位置给末尾的 nullptr
    for (const auto &s: str_args) args.push_back(s.c_str());
    args.push_back(nullptr); // 指令数组必须以 nullptr 结尾
    ctx->command(args.data());
}

/**
 * 设置选项（字符串类型）。只能在 init 之前调用某些选项，部分可以在运行中热修改。
 */
JNIEXPORT jint JNICALL
Java_com_fiepi_mpv_MpvLib_setOptionString(JNIEnv *env, jobject obj, jstring jname, jstring jvalue) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_RET(ctx, -1);
    const char *name = env->GetStringUTFChars(jname, nullptr);
    const char *value = env->GetStringUTFChars(jvalue, nullptr);
    int res = mpv_set_option_string(ctx->getHandle(), name, value);
    env->ReleaseStringUTFChars(jname, name);
    env->ReleaseStringUTFChars(jvalue, value);
    return res;
}

// --- 属性读写函数 (Integer, Double, Boolean, String) ---

JNIEXPORT jobject JNICALL
Java_com_fiepi_mpv_MpvLib_getPropertyInt(JNIEnv *env, jobject obj, jstring jprop) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_RET(ctx, nullptr);
    const char *prop = env->GetStringUTFChars(jprop, nullptr);
    int64_t value;
    int res = mpv_get_property(ctx->getHandle(), prop, MPV_FORMAT_INT64, &value);
    env->ReleaseStringUTFChars(jprop, prop);
    if (res < 0) return nullptr;
    // 将 C++ int64 封装为 Java Integer 对象返回
    return env->NewObject(JniHelpers::class_Integer, JniHelpers::method_Integer_init, (jint) value);
}

JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_setPropertyInt(JNIEnv *env, jobject obj, jstring jprop, jint jvalue) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_VOID(ctx);
    const char *prop = env->GetStringUTFChars(jprop, nullptr);
    int64_t val = jvalue;
    mpv_set_property(ctx->getHandle(), prop, MPV_FORMAT_INT64, &val);
    env->ReleaseStringUTFChars(jprop, prop);
}

JNIEXPORT jobject JNICALL
Java_com_fiepi_mpv_MpvLib_getPropertyDouble(JNIEnv *env, jobject obj, jstring jprop) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_RET(ctx, nullptr);
    const char *prop = env->GetStringUTFChars(jprop, nullptr);
    double value;
    int res = mpv_get_property(ctx->getHandle(), prop, MPV_FORMAT_DOUBLE, &value);
    env->ReleaseStringUTFChars(jprop, prop);
    if (res < 0) return nullptr;
    return env->NewObject(JniHelpers::class_Double, JniHelpers::method_Double_init, (jdouble) value);
}

JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_setPropertyDouble(JNIEnv *env, jobject obj, jstring jprop, jdouble jvalue) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_VOID(ctx);
    const char *prop = env->GetStringUTFChars(jprop, nullptr);
    mpv_set_property(ctx->getHandle(), prop, MPV_FORMAT_DOUBLE, &jvalue);
    env->ReleaseStringUTFChars(jprop, prop);
}

JNIEXPORT jobject JNICALL
Java_com_fiepi_mpv_MpvLib_getPropertyBoolean(JNIEnv *env, jobject obj, jstring jprop) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_RET(ctx, nullptr);
    const char *prop = env->GetStringUTFChars(jprop, nullptr);
    int value;
    int res = mpv_get_property(ctx->getHandle(), prop, MPV_FORMAT_FLAG, &value);
    env->ReleaseStringUTFChars(jprop, prop);
    if (res < 0) return nullptr;
    return env->NewObject(JniHelpers::class_Boolean, JniHelpers::method_Boolean_init, (jboolean) (value != 0));
}

JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_setPropertyBoolean(JNIEnv *env, jobject obj, jstring jprop, jboolean jvalue) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_VOID(ctx);
    const char *prop = env->GetStringUTFChars(jprop, nullptr);
    int val = jvalue ? 1 : 0;
    mpv_set_property(ctx->getHandle(), prop, MPV_FORMAT_FLAG, &val);
    env->ReleaseStringUTFChars(jprop, prop);
}

JNIEXPORT jstring JNICALL
Java_com_fiepi_mpv_MpvLib_getPropertyString(JNIEnv *env, jobject obj, jstring jprop) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_RET(ctx, nullptr);
    const char *prop = env->GetStringUTFChars(jprop, nullptr);
    char *value;
    int res = mpv_get_property(ctx->getHandle(), prop, MPV_FORMAT_STRING, &value);
    env->ReleaseStringUTFChars(jprop, prop);
    if (res < 0) return nullptr;
    jstring jval = env->NewStringUTF(value);
    // mpv_get_property 返回的字符串必须手动释放
    mpv_free(value);
    return jval;
}

JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_setPropertyString(JNIEnv *env, jobject obj, jstring jprop, jstring jvalue) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_VOID(ctx);
    const char *prop = env->GetStringUTFChars(jprop, nullptr);
    const char *val = env->GetStringUTFChars(jvalue, nullptr);
    mpv_set_property(ctx->getHandle(), prop, MPV_FORMAT_STRING, &val);
    env->ReleaseStringUTFChars(jprop, prop);
    env->ReleaseStringUTFChars(jvalue, val);
}

/**
 * 监听属性。当属性发生变化时，内核会发送 MPV_EVENT_PROPERTY_CHANGE 事件。
 */
JNIEXPORT void JNICALL
Java_com_fiepi_mpv_MpvLib_observeProperty(JNIEnv *env, jobject obj, jstring jprop, jint format) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_VOID(ctx);
    const char *prop = env->GetStringUTFChars(jprop, nullptr);
    mpv_observe_property(ctx->getHandle(), 0, prop, (mpv_format) format);
    env->ReleaseStringUTFChars(jprop, prop);
}

/**
 * 抓取缩略图。
 * 这是一个复杂的函数，涉及到创建软件渲染上下文、分配 Android Bitmap 以及内存数据拷贝。
 */
JNIEXPORT jobject JNICALL
Java_com_fiepi_mpv_MpvLib_grabThumbnail(JNIEnv *env, jobject obj, jint dimension) {
    MpvContext *ctx = get_ctx(env, obj);
    CHECK_MPV_INIT_RET(ctx, nullptr);

    // 获取软件渲染句柄
    mpv_render_context *render_ctx = ctx->getRenderContext();
    if (!render_ctx) return nullptr;

    // 获取视频原始宽高
    int64_t w = 0, h = 0;
    if (mpv_get_property(ctx->getHandle(), "width", MPV_FORMAT_INT64, &w) < 0 || w <= 0) return nullptr;
    if (mpv_get_property(ctx->getHandle(), "height", MPV_FORMAT_INT64, &h) < 0 || h <= 0) return nullptr;

    // 计算缩略图目标尺寸（保持宽高比）
    double scale = (double) dimension / (double) (w > h ? w : h);
    int thumb_w = (int) ((double) w * scale);
    int thumb_h = (int) ((double) h * scale);

    if (thumb_w <= 0 || thumb_h <= 0) return nullptr;

    // 创建 Android Bitmap 对象 (ARGB_8888)
    jobject config = env->GetStaticObjectField(JniHelpers::class_BitmapConfig, JniHelpers::field_ARGB_8888);
    jobject bitmap = env->CallStaticObjectMethod(JniHelpers::class_Bitmap, JniHelpers::method_Bitmap_create, thumb_w, thumb_h, config);

    // 锁定 Bitmap 像素缓冲区
    void *pixels = nullptr;
    if (AndroidBitmap_lockPixels(env, bitmap, &pixels) < 0) return nullptr;

    // 配置渲染参数，通知 mpv 将当前帧渲染到 Bitmap 的内存地址
    int size[] = {thumb_w, thumb_h};
    size_t pitch = (size_t) thumb_w * 4; // ARGB 每行字节数
    mpv_render_param params[] = {
            {MPV_RENDER_PARAM_SW_SIZE, size},
            {MPV_RENDER_PARAM_SW_FORMAT, (void *) "rgb0"}, // 使用 rgb0 兼容模式
            {MPV_RENDER_PARAM_SW_STRIDE, &pitch},
            {MPV_RENDER_PARAM_SW_POINTER, pixels},
            {MPV_RENDER_PARAM_INVALID, nullptr}
    };
    int err = mpv_render_context_render(render_ctx, params);

    // 解锁像素缓冲区并返回
    AndroidBitmap_unlockPixels(env, bitmap);
    return (err >= 0) ? bitmap : nullptr;
}

} // extern "C"
