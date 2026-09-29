# IsekaiPlayer Agent Instructions

You are an expert Android developer specializing in modern video playback applications. You are assisting in the development of **IsekaiPlayer**, a media player built with Jetpack Compose, supporting both **mpv** and **ExoPlayer (Media3)** engines.

## 🏗 Project Architecture

The project follows **Clean Architecture** principles and is divided into a multi-module Gradle structure to ensure separation of concerns:

### 1. Domain Module (`:domain` - Pure Kotlin)
- **Models**: Pure data classes (e.g., `MediaFile`, `MediaPlaybackState`, `EngineCapabilities`).
- **Repositories**: Interfaces defining data and preference operations.
- **UseCases**: Granular business logic components.
- **Player Abstraction**: `PlayerEngine` and `MediaPlayer` interfaces.
- **Service Interfaces**: `MediaStreamServer` interface for remote streaming.

### 2. Data Module (`:data` - Android Library)
- **Implementations**: Repository implementations for Local, SMB, FTP, and WebDAV.
- **Persistence**: Room (History) and typed DataStore (Preferences with JSON serializers).
- **Network Implementation**: `MediaStreamServerImpl` (Ktor-based SMB proxy).

### 3. Player Module (`:player` - Android Library)
- **Adapter Layer**: Bridges the Domain abstractions to the Native/Android Engines.
- **Implementations**: `MpvPlayerEngine`, `ExoPlayerEngine`, and `MediaPlayerImpl`.
- **UI Views**: `PlayerSurfaceView` (SurfaceView), `PlayerTextureView` (TextureView), and `PlayerSurface` (Compose bridge).
- **Logic**: Volume mapping, reactive engine hot-switching, history saving coordination, and lifecycle reference counting.

### 4. Native Engine Module (`:libmpv`)
- **JNI**: True multi-instance JNI bindings for **libmpv**.
- **Controller**: `MpvController` manages the `mpv_handle`.
- **Safety**: Pure native bridge without UI View coupling, thread-safe interaction via `lifecycleLock` and `surfaceMutex`.

### 5. FFmpeg Extension Module (`:libffmpeg`)
- **JNI**: C++ JNI bridge (`media_ffmpeg_jni.cc`) providing FFmpeg software audio decoding capabilities for ExoPlayer.
- **Decoders & Renderers**: `FFmpegLibrary`, `FFmpegAudioDecoder`, and `FFmpegAudioRenderer` for Media3/ExoPlayer integration.

### 6. App Module (`:app`)
- **UI Layer**: 100% Jetpack Compose with Material 3 and MVI pattern (ViewModels).
- **Navigation**: Modern key-based routing using **Navigation3**.
- **Dependency Injection**: Koin modules aggregating all sub-project configurations.

---

## ⚙️ Development Environment

- **Minimum SDK**: **33 (Android 13)**.
- **Modern API Usage**: Since the `minSdk` is high, avoid boilerplate backward compatibility checks (e.g., `if (SDK_INT >= 33)`). Use modern Android APIs directly.
- **Aggressive Tech Stack**: Technology choices and implementation details can be more aggressive/modern, favoring the latest Jetpack libraries and Kotlin features.

### 1. State Management & Multi-Engine Architecture
- **Optimistic Updates**: Update UI state immediately in response to user intents before waiting for background/native confirmation (especially for Play/Pause and Seek).
- **Engine Capabilities (`EngineCapabilities`)**: UI controls observe `capabilities` from `MediaPlaybackState` to conditionally enable, disable, or hide features depending on the active engine (e.g., disabling frame-step backward or MPV OSD under ExoPlayer).
- **Reactive Engine Hot-Switching**: `MediaPlayerImpl` observes `PlaybackOptions.engineType` via `GetPlaybackOptionsUseCase` and seamlessly switches between `MpvPlayerEngine` and `ExoPlayerEngine` at runtime without losing playback position.
- **Lazy Engine Initialization**: Keep `currentEngineType` in `PlayerEngineManager` initially `null`. Never default fallback to MPV or ExoPlayer prior to DataStore preference loading on cold start. Always wait for `playbackOptionsFlow.first()` to create the target engine directly and avoid wasteful engine creation/switching.
- **Component Composition & Callback Decoupling**: Sub-components under `com.fiepi.media.player.component` (`PlayerTrackManager`, `PlayerSurfaceManager`, `PlayerAudioManager`) must NEVER hold direct references to `PlayerEngine`. All engine commands must be dispatched via callbacks or delegates to `PlayerEngineManager`.

### 2. Player Logic (MediaPlayer)
- **Delegation**: Business logic (volume mapping, history saving, scrubbing behavior) must live in `MediaPlayerImpl`, NOT in the ViewModel.
- **Independent Dual Volume Control**: System volume and player volume are adjusted independently via separate volume controls. Operate on raw integers for system volume to avoid precision loss. Player volume is adjusted per engine capabilities (0–200% for `mpv` with software gain, 0–100% for `ExoPlayer`).
- **Reference Counting**: Use `incrementClientCount(PlayerClientType)` and `decrementClientCount(PlayerClientType)` to manage lifecycle.
    - `UI` clients trigger automatic surface detachment on exit.
    - Native/Engine destruction occurs only when both `UI` and `BACKGROUND` counts reach zero.

### 3. Native & Lifecycle Safety
- **ANR Prevention**: All potentially blocking release operations (SMB closing, native destruction) MUST be moved to a background `CoroutineScope` (e.g., `releaseScope` in `MediaPlayerImpl`).
- **fdsan Protection**: Follow a strict synchronous sequence in `MpvController.destroy()` to prevent crashes:
    1.  Set `isDestroyed` flag (atomic).
    2.  Set `vid=no` and `vo=null`.
    3.  Call `detachSurface()`.
    4.  Call `mpv_terminate_destroy()`.
- **Surface Mutex**: All operations involving the Android `Surface` (`setSurface`, `detachSurface`, and release-time cleanup) MUST be serialized using the `surfaceMutex` in `MediaPlayerImpl` (located in the `:player` module) to prevent race conditions during Activity transitions.
- **Identity Verification**: When detaching, always verify if the surface requesting detachment is still the one currently active (`lastSurfaceObject === surface`).

### 4. Remote Streaming (SMB)
- **Connection Reuse**: Reuse `SMBClient` and `DiskShare` via `SmbStreamSession`.
- **Handle Reuse**: Reuse file handles across HTTP Range requests to minimize network round-trips.
- **IO Performance**: Use a large buffer (e.g., 1MB) for `ResilientSmbStreamContent`.

---

## 📜 Tooling & Scripts

- **`scripts/versions.sh`**: Centralized source for all native dependency versions.
- **`scripts/sync-sources.sh`**: Idempotent script to sync/clone external git repositories (supports single-component target filtering and fast mode).
- **`scripts/build-native.sh`**: Main script for compiling native libraries across ABIs.

---

## ⚠️ Critical Rules for AI Agents

- **Module Purity**: The `:domain` module MUST remain a **pure Kotlin JVM module**. Never add Android dependencies (like `Context`, `Bitmap`, `Uri`) to this module. Use abstractions (like `Any?` or `ByteArray`) if necessary.
- **Don't Over-Simplify**: The playback logic (e.g., `handleEngineEvent` in `MediaPlayerImpl`) contains many fixes for race conditions. Before refactoring, read the comments to understand why a specific check (like `isLoaded` or `abs(pos - target) < 2000`) exists.
- **Preserve Documentation**: NEVER delete or over-simplify comments and documentation within the code. Existing comments often capture critical fixes for race conditions, lifecycle safety, and complex architectural decisions that are not immediately obvious.
- **Comment Formatting**: All code comments, KDoc documentation, and inline explanations MUST be written in **English**. Do NOT use numerical prefixes or step numbers in comments (e.g., avoid `// 1.`, `// --- 1. ---`).
- **String Management**: Always put user-facing strings in `strings.xml` in the `:app` module.
- **Dependency Injection**: Use Koin `factoryOf` or `singleOf` with proper `named` qualifiers to avoid type erasure issues when multiple instances of the same interface (like `DataStore`) exist.
- **Task Stack Stability**: Keep `MainActivity` as `standard` or `singleTop` and `PlayerActivity` as `singleTop`. NEVER use `singleTask` for `MainActivity` if it sits at the bottom of the playback task stack, as it will force-finish the player on re-entry.
- **Logging**: Use standard Android `Log` with a local `isDebug` check (based on `BuildConfig.DEBUG` or `FLAG_DEBUGGABLE`) to ensure logs are stripped in release builds.
- **Naming Conventions**: For `kotlinx.serialization` (e.g., `@SerialName`) and Room entity annotations (e.g., `@ColumnInfo`), always use **snake_case** (underscore_separated) for the name values.
- **UI Components**: For list layouts, always use `SegmentedListItem` instead of the standard `ListItem` unless there is a specific reason not to (e.g. infinite scrolling without clear boundaries).
- **Storage Unit Formatting**: For formatting file and storage sizes in the UI, always use the Android system API `android.text.format.Formatter.formatShortFileSize(context, bytes)` (or `formatFileSize(context, bytes)`) rather than custom string formatting functions.
- **Date & Time Formatting**: For date and time formatting, always use modern Java 8+ time APIs (`java.time.LocalDate`, `java.time.LocalDateTime`, `java.time.ZonedDateTime`, `java.time.format.DateTimeFormatter`) or `kotlinx-datetime` Kotlin APIs. Do NOT use legacy `java.text.SimpleDateFormat` or `java.util.Date`.
- **Compose Modifier Parameter**: For Compose components, always place the `modifier` parameter as the first parameter.
