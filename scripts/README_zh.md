[English Version](README.md)

# IsekaiPlayer 构建与工具脚本指南

本目录包含用于管理、同步、配置和编译 IsekaiPlayer 底层原生 C/C++ 依赖库（包括 `libmpv`、`FFmpeg`、`libplacebo`、`libass` 等）的核心脚本，以及生成开源库元数据信息的工具。

---

## 📁 目录结构

| 文件 / 目录 | 说明 |
| --- | --- |
| `build-native.sh` | 原生库编译与清理主入口脚本（支持智能参数解析、灵活参数顺序及 ABI 别名） |
| `env.sh` | 环境变量配置脚本（自动解析 `gradle/config.versions.toml` 中的 NDK 版本与 MinSDK，配置交叉编译工具链与参数） |
| `versions.sh` | 统一定义原生 C/C++ 依赖库仓库地址、Tag/Branch、License 及其元数据，并联动 `gradle/libs.versions.toml` |
| `sync-sources.sh` | 幂等同步、克隆与更新 `external/sources/` 下的原生依赖库源码（支持指定单组件及快速模式） |
| `update-cacert.sh` | 自动下载、校验 SHA256 并更新 `player/src/main/assets/cacert.pem` 的 Mozilla CA 根证书包工具 |
| `utils.sh` | 通用工具函数（Meson Cross File 生成器、`vulkan.pc` 生成器以及 `libraries.json` 生成器） |
| `components/` | 各 C/C++ 依赖库的单独编译脚本（`mbedtls.sh`, `lua.sh`, `ffmpeg.sh`, `mpv.sh` 等 16 个组件） |

---

## 🛠️ 环境要求

在运行脚本前，请确保开发环境满足以下条件：

### 1. 环境变量
- `ANDROID_HOME`：必须配置并指向本地 Android SDK 目录（例如 `export ANDROID_HOME=$HOME/Android/Sdk`）。

### 2. NDK 版本匹配
- 脚本会自动读取 `gradle/config.versions.toml` 中的 `android-ndk` 与 `android-minSdk`。
- 请确保 `$ANDROID_HOME/ndk/<NDK_VERSION>` 目录已安装且存在。

### 3. 宿主机工具链
支持 Linux / macOS 宿主机环境，需要安装以下命令行工具：
- **Git** / **Bash**
- **构建工具**：`make`, `cmake`, `meson`, `ninja`, `pkg-config`, `autoconf`, `automake`, `libtool`, `gperf`, `yasm`, `nasm`

---

## 🚀 原生库编译主脚本 (`build-native.sh`) 使用指南

`build-native.sh` 实现了智能参数解析与标准化，支持零参数默认构建、灵活的参数位置以及 ABI 别名。

### 命令语法格式

```bash
./scripts/build-native.sh [arch] [action]
./scripts/build-native.sh [action] [arch]
# 特例命令：
./scripts/build-native.sh clean [--src] [--so]
./scripts/build-native.sh libraries
./scripts/build-native.sh -h | --help
```

---

### 参数说明

#### 1. 架构参数 `[arch]`

| `arch` 参数值 | 智能识别别名 | 对应的 Android ABI | 说明 |
| --- | --- | --- | --- |
| `arm64` *(默认)* | `arm64-v8a` | `arm64-v8a` | 64位 ARM 架构 |
| `armv7l` | `armeabi-v7a`, `armv7`, `armv7a` | `armeabi-v7a` | 32位 ARM 架构 |
| `x86` | `x86` | `x86` | 32位 x86 模拟器架构 |
| `x86_64` | `x86_64` | `x86_64` | 64位 x86_64 模拟器架构 |
| `32` | - | `armeabi-v7a` + `x86` | 批量构建所有 32 位架构 |
| `64` | - | `arm64-v8a` + `x86_64` | 批量构建所有 64 位架构 |
| `all` | - | 4 种 ABI 全选 | 批量构建所有 4 种支持的架构 |

#### 2. 组件/动作参数 `[action]`

| `action` 参数值 | 说明 |
| --- | --- |
| `all` *(默认)* | 按依赖顺序依次编译所有 16 个 C/C++ 组件 |
| 组件精确名称 | 仅编译单个指定组件，可选值为：<br>`mbedtls`, `lua`, `dav1d`, `libxml2`, `freetype2`, `fribidi`, `harfbuzz`, `unibreak`, `fontconfig`, `libass`, `curl`, `libwebp`, `shaderc`, `ffmpeg`, `libplacebo`, `mpv` |

---

### 使用示例

#### 1) 基础与默认编译
```bash
# 默认构建：直接运行不加参数，默认编译 arm64-v8a 架构的所有组件
./scripts/build-native.sh

# 单独传组件名：默认针对 arm64-v8a 编译指定组件
./scripts/build-native.sh ffmpeg
./scripts/build-native.sh mpv

# 单独传架构名：编译指定架构的所有组件
./scripts/build-native.sh arm64
./scripts/build-native.sh armeabi-v7a
./scripts/build-native.sh all
```

#### 2) 组合编译 (支持双向顺序)
```bash
# 标准顺序 [架构] [组件]
./scripts/build-native.sh arm64 ffmpeg
./scripts/build-native.sh armv7l mpv

# 灵活顺序 [组件] [架构]
./scripts/build-native.sh ffmpeg arm64
./scripts/build-native.sh mpv 64
```

#### 3) 清理指令
```bash
# 完全清理 (中间构建文件 + prebuilt 输出库)
./scripts/build-native.sh clean

# 仅清理 sources/ 下的中间构建目录 (build_android, config.log 等)
./scripts/build-native.sh clean --src

# 仅清理 external/prebuilt/ 下已编译的 .so/.a 与头文件
./scripts/build-native.sh clean --so
```

#### 4) 开源元数据生成
```bash
# 仅更新 external/prebuilt/assets/libraries.json，不触发 C/C++ 编译
./scripts/build-native.sh libraries
```

---

## 🔄 源码管理脚本 (`sync-sources.sh`)

统一的 `sync-sources.sh` 脚本负责按照 `versions.sh` 中定义的源码仓库，自动完成首次克隆、重置构建临时补丁、拉取最新代码/切换指定分支及更新 Git 子模块。

### 命令语法格式

```bash
./scripts/sync-sources.sh [options] [target]
```

### 参数说明

- `target`：目标组件名称（如 `ffmpeg`, `mpv`, `libass` 等）或 `all`（默认，处理全部组件）。
- `-d, --download-only, --fast`：快速模式。仅下载/克隆缺失的依赖仓库，对已存在的仓库跳过更新与重置。
- `-h, --help`：显示帮助信息。

### 使用示例

```bash
# 幂等同步/更新所有原生依赖库源码
./scripts/sync-sources.sh

# 仅同步/更新单个指定组件 (例如 ffmpeg)
./scripts/sync-sources.sh ffmpeg

# 快速模式：仅克隆缺失的依赖，不拉取/重置已存在的库
./scripts/sync-sources.sh --download-only

# 对单个指定组件使用快速模式
./scripts/sync-sources.sh --fast mpv
```

---

## 🧱 C/C++ 组件构建依赖顺序

当执行 `all` 动作时，`build-native.sh` 会严格按照以下拓扑依赖顺序构建各组件，产物输出至 `external/prebuilt/<ABI>/`：

1. **`mbedtls`**：加解密与 TLS 协议库
2. **`lua`**：脚本引擎（提供 mpv 脚本运行支持）
3. **`dav1d`**：AV1 视频解码器
4. **`libxml2`**：XML 解析库
5. **`freetype2`**：字体渲染引擎
6. **`fribidi`**：Unicode 双向文本算法库 (Bidi)
7. **`harfbuzz`**：文本整形引擎
8. **`unibreak`**：Unicode 断行算法库
9. **`fontconfig`**：字体匹配与配置库
10. **`libass`**：字幕渲染引擎
11. **`curl`**：网络传输库
12. **`libwebp`**：WebP 图像格式编解码库
13. **`shaderc`**：Vulkan 着色器编译器
14. **`ffmpeg`**：音视频解复用与解码（启用 MediaCodec、JNI、mbedTLS、dav1d、libxml2、libwebp）
15. **`libplacebo`**：基于 Vulkan/OpenGL 的 GPU 图像渲染与色彩管理库
16. **`mpv`**：`libmpv` 核心播放器库
