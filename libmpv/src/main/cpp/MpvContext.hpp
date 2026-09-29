#include <mpv/client.h>
#include <mpv/render.h>
#include <string>
#include <vector>
#include <mutex>
#include <atomic>
#include <pthread.h>
#include <jni.h>

/**
 * MpvContext 是对 libmpv 核心句柄及其渲染上下文的 C++ 封装。
 * 它负责管理单实例 mpv 的生命周期，并持有与 Java 层对应的全局引用。
 */
class MpvContext {
public:
    /**
     * 构造函数：创建一个新的 mpv 实例，并关联对应的 Java 层 MpvLib 实例。
     * @param mpvLibInstance Java 层 MpvLib 对象的全局引用
     */
    MpvContext(jobject mpvLibInstance) : handle(mpv_create()), jInstance(nullptr) {
        this->jInstance = mpvLibInstance;
    }

    /**
     * 析构函数：负责资源的彻底释放。
     * 1. 释放渲染上下文（如果已创建）。
     * 2. 终止并销毁 mpv 核心句柄。
     */
    ~MpvContext() {
        if (render_ctx) mpv_render_context_free(render_ctx);
        if (handle) mpv_terminate_destroy(handle);
    }

    /**
     * 检查 mpv 句柄是否创建成功。
     */
    [[nodiscard]] bool isValid() const {
        return handle != nullptr;
    }

    /**
     * 执行 mpv 实例的正式初始化。
     * 必须在配置完基础选项（如 vo, gpu-api）后调用。
     */
    int initialize() {
        return mpv_initialize(handle);
    }

    /**
     * 设置 mpv 选项字符串（Key-Value 形式）。
     */
    void setOptionString(const std::string &name, const std::string &value) {
        mpv_set_option_string(handle, name.c_str(), value.c_str());
    }

    /**
     * 发送 mpv 指令。
     * @param args 以 nullptr 结尾的字符串数组
     */
    void command(const char **args) {
        mpv_command(handle, args);
    }

    /** 获取原始 mpv 句柄 */
    mpv_handle *getHandle() {
        return handle;
    }

    /** 获取对应的 Java 实例全局引用 */
    jobject getJInstance() {
        return jInstance;
    }

    /**
     * 获取或按需创建软件渲染上下文。
     * 目前主要用于实现 grabThumbnail（截图）功能。
     */
    mpv_render_context *getRenderContext() {
        if (!render_ctx) {
            // 配置为软件渲染模式 (SW)，以便将帧直接输出到内存 Buffer 中
            mpv_render_param params[] = {
                    {MPV_RENDER_PARAM_API_TYPE, (void *) MPV_RENDER_API_TYPE_SW},
                    {MPV_RENDER_PARAM_INVALID, nullptr}
            };
            if (mpv_render_context_create(&render_ctx, handle, params) < 0) {
                return nullptr;
            }
        }
        return render_ctx;
    }

    // --- 线程与状态标记 ---

    /** 原子标记：请求事件循环线程退出 */
    std::atomic<bool> event_thread_request_exit{false};

    /** 事件循环线程 ID */
    pthread_t event_thread_id{0};

    /** 缓存当前的 Surface 全局引用，用于 attach/detach 识别 */
    jobject g_surface{nullptr};

    /** 记录上一次发送给 Java 层的 time-pos 进度值（秒），用于节流 */
    double last_sent_time_pos{-1.0};

    /** 记录上一次发送给 Java 层的 demuxer-cache-duration 值（秒），用于节流 */
    double last_sent_demuxer_cache{-1.0};

private:
    /** libmpv 核心句柄 */
    mpv_handle *handle;

    /** 渲染上下文（用于截图） */
    mpv_render_context *render_ctx{nullptr};

    /** 对应的 Java 实例 */
    jobject jInstance;

    /** 内部保护互斥锁 */
    std::mutex mtx;
};
