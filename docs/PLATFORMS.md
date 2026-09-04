# 平台接入指南（iOS / 鸿蒙 / 运行与验证）

> 本仓库在 Windows 上可直接验证 **H5（浏览器）** 与 **Android（构建 APK）**。
> iOS 需 macOS+Xcode，鸿蒙需 DevEco Studio —— 本指南给出接入步骤。
> 官方参考库已克隆到 `E:\Projects\github\kuikly_work\KuiklyUI`（下文 `<R>`）。

## 版本

| 项 | 值 |
|---|---|
| Kuikly 运行库（core / core-annotations / compose / core-ksp） | `2.27.0-2.1.21` |
| Kuikly **gradle 插件** | `2.14.1-2.0.21`（官方仓库同样固定此版本以兼容 Gradle 7.6 脚本编译器） |
| Kotlin / AGP / Gradle / JDK | `2.1.21` / `7.4.2` / `7.6.3` / 17（wrapper 用腾讯镜像发行版） |
| Web 渲染 | `com.tencent.kuikly-open.core-render-web:{base,h5}:2.27.0-2.1.21` |
| Android 渲染 | `com.tencent.kuikly-open:core-render-android:2.27.0-2.1.21` |

仓库源：`mirrors.tencent.com/nexus/repository/maven-tencent/`（2.5.0 后官方构件只在此镜像与内部）。

## Android（本机可验证）

```powershell
# 首次构建（会自动下载 platform-34 等 SDK 组件）
setx JAVA_HOME "E:\Projects\github\kuikly_work\tools\jdk-17.0.20.1+1"   # 或临时 export
.\gradlew.bat :androidApp:assembleDebug
# 产物：androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

- 有可用 AVD：`emulator -avd Medium_Phone` 启动后 `adb install` 安装；数据在 app filesDir/capsules.json。
- 系统分享（导出）与系统文件选择（导入）由 `androidApp` 壳经 `CapBridge` 注入实现。

## H5（浏览器实时预览/静态托管）

开发预览：

```powershell
npm install   # 仅当用根 package.json 静态服务时需要；本仓库未带则跳过
.\gradlew.bat :shared:packLocalJsBundleDebug
.\gradlew.bat :h5App:prepareDevBusiness
.\gradlew.bat :h5App:jsBrowserDevelopmentRun -t     # dev server :8080
# 浏览器 http://localhost:8080/?page_name=home
```

静态产物（任意静态服务器托管）：

```powershell
# 注：分两步执行（同一次调用中 pack 与 publish 无依赖会乱序，导致 page 为空）
.\gradlew.bat :shared:packLocalJSBundleRelease
.\gradlew.bat :h5App:publishLocalJSBundle
# 用仓库自带零依赖静态服务器：
node scripts\static-server.mjs 8080
# 打开 http://localhost:8080/?page_name=home
```

开发预览（dev-server 不含业务 page，仅用于壳代码热更；业务包需先解到 processedResources）：

```powershell
.\gradlew.bat :shared:packLocalJsBundleDebug
.\gradlew.bat :h5App:prepareDevBusiness
.\gradlew.bat :h5App:jsBrowserDevelopmentRun -t
```

> 说明：`shared` 被 Kuikly 插件打成 `nativevue2` 业务 JS 包，由 h5App 壳在同页加载；
> 持久化走 `window.localStorage["snap_capsules"]`，导出=浏览器下载，导入=文件选择。

> ⚠️ **H5 预览当前状态（2026-09）**：宿主壳(h5App)与业务(nativevue2)是两个独立 Kotlin/JS bundle，
> 各自 UMD 会把自身的点分命名空间导出到 `window.com.*`，壳加载时会**覆盖**业务挂在
> `window.com.tencent.kuikly.core.nvi` 上的 `registerCallNative`（壳不含该 nvi），导致
> `registerCallNative … reading 'registerCallNative'` → 业务回退调用 `callNative`（未定义）→ 白屏。
> 业务单独加载时一切就位（headless 验证过）。需框架侧单 bundle 形态或官方模板一致打包才能点亮；
> 本仓库 Android 端已完整可视化验证（见 `docs/screenshots/`），H5 建议以官方脚手架的打包链路为准复测。

## iOS（需 macOS + Xcode + CocoaPods）

1. 在 `shared/build.gradle.kts` 启用 iOS target 与 cocoapods 块（参照 `<R>/demo/build.gradle.kts`），并补充
   `shared/src/iosMain/kotlin/com/snapcapsule/data/Storage.ios.kt` 的 `CapsuleFileStorage` actual：
   用 `NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, …)` 读写 `capsules.json`（NSString writeToFile / NSFileManager）。
2. 按官方搭建壳：复制 `<R>/iosApp` 的 Xcode 工程骨架；`Podfile` 加入
   `pod 'OpenKuiklyIOSRender', '<版本 2.27.0-2.1.21>'` 与本工程 `shared` 的 local pod；
   首次 `pod install --repo-update`。
3. 容器 VC 抄 `<R>/iosApp/.../KuiklyRenderViewController`，`fetchContextCodeWithPageName:` 返回 framework 名 `"shared"`；
   打开页面 `pageName = "home"`。
4. Xcode 若报沙盒权限错：Target → Build Settings → 将 **User Script Sandboxing** 设为 `No`。
5. iOS 导出/导入：在壳的 `CapBridge` 注入（`UIActivityViewController` / `UIDocumentPickerViewController`）。

## 鸿蒙（需 DevEco Studio，HarmonyOS NEXT API 12+）

1. shared 需切鸿蒙工具链（官方方式）：使用 `settings.ohos.gradle.kts` + Kotlin `2.0.21-KBA-010`
   定制工具链 + 依赖后缀 `*-ohos`（`2.27.0-2.0.21-ohos` 一类），Windows 需 `OHOS_SDK_HOME` 指向
   DevEco 的 openharmony sdk。详见 `<R>/docs/DevGuide/harmony-dev.md` 与官方 `harmony.md`。
2. 壳工程抄 `<R>/ohosApp`：entry `oh-package.json5` 加 `"@kuikly-open/render": "<版本>"`；
   `Index.ets` 用 `Kuikly(pageName="home", …)` 承载；`EntryAbility` 初始化适配器并下发 cacheDir。
3. 编译 shared → `libshared.so` 拷入 entry C++ 层，CMake 链接后由 DevEco 出 HAP。
4. DevEco 打开 `ohosApp` 后需签名（File → Project Structure → Signing Configs）。

## 数据文件

- JSON schema v1：`{ "version": 1, "capsules": [ { id, text, cat: "work"|"life", tags[], createdAt, status: "active"|"archived" } ] }`
- Android 落点 `filesDir/capsules.json`；H5 落点 `localStorage["snap_capsules"]`。
- 导入为「全量校验，合法才落盘」；非法文件会拒绝并保留原数据。
