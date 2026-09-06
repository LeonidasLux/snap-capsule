# 闪念胶囊 · H5 启动指南（浏览器）

> 面向本机（Windows 11）开发环境：把「闪念胶囊」业务（`shared/src/commonMain`）以 **H5（浏览器）** 形态构建、启动与验证。
> Android 端启动见根目录 `STARTUP.md`；iOS / 鸿蒙接入与跨端数据约定见 `docs/PLATFORMS.md`。

## 环境速览（本机实测 2026-09）

| 项 | 值 |
|---|---|
| Kotlin / Kuikly | `2.1.21`；kuikly compose `2.27.0-2.1.21`；Web 渲染 `core-render-web:{base,h5}` 同版本 |
| dev server | `http://localhost:8080/`（`jsBrowserDevelopmentRun` 起的 dev server） |
| 页面入口 | `http://localhost:8080/?page_name=home` |
| 业务包 | `shared` 被 Kuikly 插件打成 `nativevue2.zip`，再解压进 h5App 的 `processedResources/js/main/page/` |
| 持久化 | `window.localStorage["snap_capsules"]`（JSON schema v2，与 Android 的 `files/capsules.json` 彼此独立） |
| 导出 / 导入 | 导出=浏览器下载；导入=文件选择（经 `CapBridge` 注入） |
| 演示数据 | 列表为空时 → 主页右上 ⚙️ 设置 →「🧹 载入示例数据」（一次 50 条、跨三年：未完成/已完成/回收站 三态都有内容） |

## 一、开发预览（推荐）

三条 gradle 命令**分开执行**（不要合成一条 `./gradlew A B`，见下「常见坑」）：

```powershell
.\gradlew.bat :shared:packLocalJsBundleDebug     # 1. shared 打成最新业务包 nativevue2.zip
.\gradlew.bat :h5App:prepareDevBusiness          # 2. 解压到 h5App 的 dev page
.\gradlew.bat :h5App:jsBrowserDevelopmentRun -t  # 3. dev server :8080（保持运行）
```

浏览器打开：

```
http://localhost:8080/?page_name=home
```

> 改了 `shared/src/commonMain`（页面/逻辑）后：**先停掉**第 3 步的 dev server → 重跑 1、2 → 重跑 3。
> 业务页是 dev server **启动那一刻**读入的静态解压物，`-t` 只热更 h5App 壳代码，不会追业务包的变化。

## 二、静态托管（release 形态）

```powershell
.\gradlew.bat :shared:packLocalJSBundleRelease   # 先单独打 release 业务包
.\gradlew.bat :h5App:publishLocalJSBundle        # 再产出 build/dist/js/productionExecutable
node scripts\static-server.mjs 8080
```

打开同上：`http://localhost:8080/?page_name=home`

> release 的 pack 与 publish 同样**分两次执行**（同一次调用中二者无依赖会并行乱序，导致 page 为空/旧）。

## 三、无头 DOM 核对（验证 UI 落点/数据）

Kuikly 的 H5 端把界面渲染成**真实 DOM（非 canvas）**，文本可直接 `getBoundingClientRect()` / `elementFromPoint()` 断言，无需 OCR。
本仓库此前用 headless Chrome + CDP 做过同类验证，脚本范式见 `scripts/cdp-check-sample.mjs`（点「载入示例数据」并从 `localStorage` 校验落盘）。
需要更顺手的驱动时可用全局 playwright（`npm i -g playwright` + 系统 Chrome 的 `executablePath`）。

示例断言（「空内容点保存」的顶部提示落在 y≈48px、且未被弹窗遮罩盖住）：

- 前置：dev server 在 `:8080`
- 打开 `/?page_name=home` → 点 FAB `＋` → **不输入**直接点「保存」
- 找文本「先记点什么吧」，取其黑色底容器（`Palette.fg`）的 `getBoundingClientRect().top`
- 预期 ≈ 48（视口 dpr=1 时 `48.dp`）；并用 `document.elementFromPoint(文字中心)` 命中 toast 自身，即证明它压在新建弹窗之上

实测截图：`docs/screenshots/h5-toast-top-48-empty-save.png`（390×844 视口，提示块 top=48px，弹窗保持打开、未新增卡片）。

## 四、数据：查看 / 清空 / 注入

DevTools Console：

```js
JSON.parse(localStorage['snap_capsules'])                       // 查看
localStorage.removeItem('snap_capsules'); location.reload()     // 清空
```

UI 内：列表为空 → ⚙️ 设置 →「载入示例数据」；「清空所有数据」复位。
> ⚠️ H5 与 Android 数据各自独立（`localStorage` vs app 私有目录 `files/capsules.json`）。

## 常见坑

- **pack 与 prepare/publish 必须分两条 gradle 命令**：同一次调用里任务间无依赖会并行乱序，prepare 可能抢先解压到**上一轮的旧业务包**，于是 dev server 给的是旧页面（页面元素「改了很多还是旧的」）。自查：`h5App\build\processedResources\js\main\page\nativevue2.js` 的修改时间应**不早于** `shared\build\outputs\kuikly\js\debug\local\nativevue2.zip`。
- **改了 shared 仍看旧 UI**：除上一条外，dev server 只认启动时的 page，先停 server 再按「一」重来。
- **H5 白屏的历史记录**：`docs/PLATFORMS.md` 曾记壳与业务双 bundle 冲突白屏。**dev server 形态已实测正常**（DOM 可读、可点、可断言）；静态壳形态如遇白屏，以 Android 端（`STARTUP.md`）为可视化验证基准。
- **8080 被占**：`netstat -ano | findstr :8080` 定位后释放，或改端口。
- **gradle 输出乱码（GBK）**：加 `--console=plain` 可缓解，不阻塞。

---

相关：Android 启动 → [`STARTUP.md`](STARTUP.md) · iOS/鸿蒙接入 → [`docs/PLATFORMS.md`](docs/PLATFORMS.md)
