# 闪念胶囊 · 启动指南（Android）

> 面向本机（Windows 11）当前开发环境：如何把「闪念胶囊」的 app 窗口在 **Android 模拟器 / 真机** 上构建、安装并跑起来。
> H5（浏览器）启动见根目录 `H5_STARTUP.md`；iOS / 鸿蒙接入见 `docs/PLATFORMS.md`。Android 是本仓库可视化验证的主端。

## 环境速览（本机实测）

| 项 | 值 |
|---|---|
| Android SDK | `C:\Users\Administrator\AppData\Local\Android\Sdk`（见 `local.properties` 的 `sdk.dir`） |
| adb | `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` |
| emulator | `%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe` |
| AVD | `Medium_Phone`（`adb emu avd name` 可查当前实例） |
| JDK | Temurin 17，`JAVA_HOME=E:\Projects\github\kuikly_work\tools\jdk-17.0.20.1+1`（gradle daemon 用） |
| 包 / Activity | `com.snapcapsule.app` / `com.snapcapsule.app.ShellActivity`（应用 id `com.snapcapsule.app`，versionName `0.1.0`） |
| APK 产物 | `androidApp\build\outputs\apk\debug\androidApp-debug.apk` |
| 运行数据 | app 私有目录 `files/capsules.json`（JSON schema v2） |

命令示例在 **PowerShell** 下书写；`$ADB` 请先赋值：

```powershell
$ADB = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
```

> git-bash 下需用绝对路径，并建议 `export MSYS_NO_PATHCONV=1` 关闭路径改写，
> 否则 `/data/...` 这类参数会被转成 `C:/Program Files/Git/data/...`。

## 一、构建 APK（代码改动后必做一次）

```powershell
# 首次会自动下载 platform-34 等 SDK 组件，可能较久
.\gradlew.bat :androidApp:assembleDebug
```

产物：`androidApp\build\outputs\apk\debug\androidApp-debug.apk`。
改了 `shared/` 下 commonMain 代码（页面/逻辑）或 `androidApp` 壳代码后，都重新执行这一步。

## 二、启动 Android 模拟器（若已有在跑设备可跳过）

```powershell
# 启动指定 AVD
& "$env:LOCALAPPDATA\Android\Sdk\emulator\emulator.exe" -avd Medium_Phone -no-snapshot-save -no-boot-anim
```

等待系统启动完成（返回 `1` 即就绪）：

```powershell
& $ADB wait-for-device shell 'while [ "$(getprop sys.boot_completed)" != "1" ]; do sleep 1; done; getprop sys.boot_completed'
```

确认设备在线：

```powershell
& $ADB devices     # 应见 emulator-5554  device
```

## 三、安装 / 覆盖安装

```powershell
# 方式 A：把刚构建的 APK 装到在跑设备
& $ADB install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk

# 方式 B：设备在跑时直接由 gradle 构建并安装
.\gradlew.bat :androidApp:installDebug
```

## 四、启动 app 窗口

```powershell
# 冷启动（无进程时新建任务并拉起前台）
& $ADB shell am start -n com.snapcapsule.app/.ShellActivity

# 若已在后台，仅带回前台
& $ADB shell am start -n com.snapcapsule.app/.ShellActivity
# 前台是否已是本 app：
& $ADB shell "dumpsys window | grep mCurrentFocus"   # → Window{… com.snapcapsule.app/.ShellActivity}
```

**「重启进程」干净启动**（清内存态、重新走启动读盘，UI 改动/数据验证常用）：

```powershell
& $ADB shell am force-stop com.snapcapsule.app
& $ADB shell am start  -n com.snapcapsule.app/.ShellActivity
```

回到桌面 / 返回：`& $ADB shell input keyevent 3`（HOME）/ `4`（BACK）。
点按：`& $ADB shell input tap <x> <y>`；滑动：`& $ADB shell input swipe <x1> <y1> <x2> <y2> <ms>`。

## 五、数据：位置 / 示例 / 清空

数据只落在 app 私有目录，重启模拟器（不开 wipe）仍在；卸载或 `-wipe-data` 会清掉。

```powershell
# 查看 / 读取（run-as 在可调试包上可用）
& $ADB shell run-as com.snapcapsule.app ls -la files/
& $ADB shell run-as com.snapcapsule.app cat files/capsules.json

# 覆盖写入示例前先 push 到共享区再经 run-as 写回（注意：不要直接对 /data/data 用绝对路径）
& $ADB push 你的.json /data/local/tmp/in.json
& $ADB shell run-as com.snapcapsule.app sh -c "cat /data/local/tmp/in.json > files/capsules.json"
```

更省事的演示数据入口：**列表为空时**，进主页右上角 ⚙️ 设置 →「🧹 载入示例数据」，一次填入 50 条、时间横跨三年（未完成为主，含已完成与 3 条回收站，JsonCodec.loadSample 生成），让「未完成 / 已完成 / 回收站」都有内容。

> ⚠️ 直接写文件注入的前提是先 `am force-stop` 再启动，否则进程内存态会盖回旧数据；
> 用「设置 → 清空所有数据」清空后，再点「载入示例数据」即可，无需 adb 写盘。

## 六、截图取证（验证 UI 用）

```powershell
# PowerShell 版
& $ADB exec-out screencap -p > shot.png

# git-bash 版（MSYS_NO_PATHCONV=1 下）：
"$ADB" shell screencap -p /data/local/tmp/shot.png && "$ADB" pull /data/local/tmp/shot.png shot.png
```

> 不要用 PowerShell 的 `>` 直接接 `adb exec-out` 的二进制输出（会被写成 UTF-16 损坏 PNG），
> 上面 `exec-out … > file` 在 PowerShell 可用；更稳妥是 shell 内写文件再 pull。

## 七、开发循环速查

1. 改代码 → `.\gradlew.bat :androidApp:assembleDebug`
2. `& $ADB install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk`
3. `& $ADB shell am force-stop com.snapcapsule.app`，再 `am start`
4. 截图/点按验证；改的是渲染层判定（完成态切换、层级、键盘等）就用「清空 → 载入示例」喂数据复现。

## 附：常见坑

- **adb 找不到**：bash 的 PATH 里没有 adb，用绝对路径或先 `export ADB=…`。
- **MSYS 改写 `/data` 路径**：见上，`MSYS_NO_PATHCONV=1`。
- **截图为空/编辑器内容不可见**：Kuikly 的弹层走页面内 slot，uiautomator dump 看不到；直接截图看像素即可。
- **软键盘相关**：`ShellActivity` 用 `adjustResize` 承载编辑器弹窗上移；模拟器 Google LatinIME 不上报 IME insets，键盘相关验证建议用真机（见仓库约定/修复记录）。
