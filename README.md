# 闪念胶囊 · snap-capsule

一套 Kotlin Multiplatform 代码实现的「闪念胶囊」笔记 App，基于腾讯 TDS 的 **Kuikly** 跨端 UI 框架，
目标平台 Android / iOS / 鸿蒙(HarmonyOS) / H5，数据为本地单 JSON 文件（导入导出同构）。

> 原型：`shannian-capsule-prototype-v2.html`（高保真 HTML MVP，交互与视觉的唯一权威来源；v1 的时间筛选/归档已被 v2 的「未完成/已完成」语义取代）。

## 架构

```
settings.gradle.kts · build.gradle.kts   版本矩阵与仓库（腾讯镜像为主）
shared/        ★ KMP 模块：全部业务与 Compose DSL UI（@Page("home") 单页状态机）
androidApp/    Android 壳（单 Activity，JVM 执行模式）—— 本机可 assembleDebug
h5App/         Web 壳（加载 shared 打出的 nativevue2 业务 JS 包）—— 本机浏览器预览
docs/PLATFORMS.md   iOS / 鸿蒙接入指南
```

- UI：Kuikly **Compose DSL**（`@Page + ComposeContainer + setContent`）；import 规则
  `androidx.compose.runtime.*` + `com.tencent.kuikly.compose.*`。
- 数据：`model/Capsule`（kotlinx.serialization，schema v2：`done`/`trashed` 双布尔 + 创建/更新/完成三时间戳，兼容读取 v1）→ `data/CapsuleStore`（snapshot 状态）
  → `CapsuleFileStorage`（expect/actual：Android=filesDir 文件、H5=localStorage）。
- 导入导出：设置页触发；H5=浏览器下载/文件选择，Android=系统分享/文件选择（由壳注入 `CapBridge`）。
- 交互：卡片点按→直接进编辑抽屉；左滑→「完成/未完成（绿）＋删除（红）」，已完成卡片置灰、脚注显示 创建于/完成于；顶栏 🗑 直达回收站；回收站条目点按→只读「查看胶囊」（复用编辑抽屉），按钮恢复/彻底删除。

## 功能

主页胶囊列表（未完成/已完成 筛选）· 空态 · 新建/编辑/查看 半屏弹窗（分类+标签；编辑态可删除此条，查看态只读）·
抽屉「更多」折叠展示 创建/更新/完成时间 · 完成/移回未完成 · 回收站(查看/恢复/彻底删除，彻底删除有确认) · 导出/导入 JSON · 数据总量 · 示例数据/清空 · Toast。

## 运行

- H5（浏览器）启动 → [`H5_STARTUP.md`](H5_STARTUP.md)（开发预览 / 静态托管 / 常见坑）
- Android 启动 → [`STARTUP.md`](STARTUP.md)
- iOS / 鸿蒙接入 → [`docs/PLATFORMS.md`](docs/PLATFORMS.md)

```powershell
# H5 静态预览（最快，分两步避免任务乱序）
.\gradlew.bat :shared:packLocalJSBundleRelease
.\gradlew.bat :h5App:publishLocalJSBundle
node scripts\static-server.mjs 8080
# 浏览器 http://localhost:8080/?page_name=home

# Android APK
.\gradlew.bat :androidApp:assembleDebug   # 产物 androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

## 版本矩阵

Kuikly 运行库 `2.27.0-2.1.21`（core/compose/core-ksp/core-render-*）· Kuikly gradle 插件 `2.14.1-2.0.21`
（Gradle 7.6 内嵌 Kotlin 1.8 脚本编译器不兼容新插件元数据，官方仓库亦固定此版本）
· Kotlin `2.1.21` · AGP `7.4.2` · Gradle `7.6.3`（wrapper，腾讯镜像发行版）· JDK 17 · KSP `2.1.21-2.0.1`。

仓库源：`mirrors.tencent.com/nexus/repository/maven-tencent/` 等（见 settings.gradle.kts）。
