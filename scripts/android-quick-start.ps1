#requires -Version 5.1
<#
  ─────────────────────────────────────────────────────────────
  闪念胶囊 · Android 快速启动脚本（Windows / PowerShell）
  ─────────────────────────────────────────────────────────────
  对应仓库根 STARTUP.md 的一键化：自动拉起模拟器 → 构建 APK
  → 安装 → 冷启动 app。任何位置执行即可，脚本内部按仓库根定位。

  用法：
    .\scripts\android-quick-start.ps1               # 全流程（无在线设备时自动启动 AVD）
    .\scripts\android-quick-start.ps1 -SkipBuild    # 跳过 gradle 构建，复用已有 APK
    .\scripts\android-quick-start.ps1 -NoRestart    # 装完不 force-stop，直接拉前台（保留内存态）
    .\scripts\android-quick-start.ps1 -NoEmulator   # 不开模拟器，要求已有在线设备/真机
    .\scripts\android-quick-start.ps1 -Avd Pixel_2  # 指定 AVD 名（默认 Medium_Phone）

  开发循环最快用法（代码只在 shared/androidApp 层改动后）：
    .\scripts\android-quick-start.ps1 -SkipBuild -NoRestart
#>
[CmdletBinding()]
param(
    [switch]$SkipBuild,    # 跳过 :androidApp:assembleDebug
    [switch]$NoRestart,    # 安装后不 force-stop，直接 am start 拉前台
    [switch]$NoEmulator,   # 不自动启动模拟器，要求已有在线设备
    [string]$Avd = 'Medium_Phone'
)

$ErrorActionPreference = 'Stop'

$repoRoot  = Split-Path -Parent $PSScriptRoot
$apk       = Join-Path $repoRoot 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'
$pkg       = 'com.snapcapsule.app'
$activity  = "$pkg/.ShellActivity"

# ───────────────────────── SDK 定位 ─────────────────────────
function Resolve-Sdk {
    # 候选顺序：LocalAppData 默认路径 → local.properties 的 sdk.dir → ANDROID_HOME
    $cand = @()
    if ($env:LOCALAPPDATA) { $cand += (Join-Path $env:LOCALAPPDATA 'Android\Sdk') }
    $lp = Join-Path $repoRoot 'local.properties'
    if (Test-Path $lp) {
        foreach ($line in Get-Content $lp) {
            if ($line -match '^sdk\.dir=') {
                $dir = $line.Substring('sdk.dir='.Length).Trim()
                $dir = $dir.Replace('\:', ':').Replace('\\', '\')  # java properties 转义还原
                if ($dir) { $cand += $dir }
            }
        }
    }
    if ($env:ANDROID_HOME) { $cand += $env:ANDROID_HOME }
    foreach ($c in $cand) { if ($c -and (Test-Path $c)) { return $c } }
    throw '未找到 Android SDK：请设置 ANDROID_HOME，或确认 local.properties / LocalAppData 路径正确'
}

function Get-OnlineDevices {
    # 返回处于 device 状态（可调试）的 serial 列表，忽略 offline/unauthorized
    $list = @()
    foreach ($line in @(& $adb devices)) {
        if ($line -match '^(\S+)\s+device$') { $list += $Matches[1] }
    }
    return $list
}

$sdk  = Resolve-Sdk
$adb  = Join-Path $sdk 'platform-tools\adb.exe'
$emu  = Join-Path $sdk 'emulator\emulator.exe'
if (-not (Test-Path $adb)) { throw "找不到 adb：$adb" }
Write-Host "SDK   : $sdk"

# ──────────────── 1/5 检查在线设备，必要时拉起模拟器 ────────────────
Write-Host "`n== 1/5 检查在线设备 ==" -ForegroundColor Cyan
$devs = @(Get-OnlineDevices)
if ($devs.Count -gt 0) {
    Write-Host ("在线设备：" + ($devs -join ', '))
} elseif ($NoEmulator) {
    throw '无在线设备且已指定 -NoEmulator：请先手动启动模拟器或连接真机'
} else {
    if (-not (Test-Path $emu)) { throw "找不到 emulator：$emu" }
    $avds = @(& $emu -list-avds)
    if ($Avd -notin $avds) { throw "AVD [$Avd] 不存在；可用：$($avds -join ', ')" }
    Write-Host "无在线设备，启动 AVD [$Avd]（模拟器窗口将弹出，首次启动可能较慢）…"
    Start-Process -FilePath $emu -ArgumentList @('-avd', $Avd, '-no-snapshot-save', '-no-boot-anim')
    Write-Host '等待设备连接…'
    & $adb wait-for-device
    Start-Sleep -Seconds 2
    $devs = @(Get-OnlineDevices)
    if ($devs.Count -eq 0) { throw '设备已连上但状态非 device（可能是 offline/unauthorized），请检查模拟器/开发者授权' }
    Write-Host ("设备就绪：" + ($devs -join ', '))
}

# ──────────────── 2/5 等待系统启动完成 ────────────────
# 真机/已 boot 的模拟器此处秒过；冷启动的模拟器轮询 sys.boot_completed==1
Write-Host "`n== 2/5 等待系统启动完成 ==" -ForegroundColor Cyan
$deadline = (Get-Date).AddSeconds(180)
$boot = ''
while ((Get-Date) -lt $deadline -and $boot -ne '1') {
    $boot = (& $adb shell getprop sys.boot_completed 2>$null | Out-String).Trim()
    if ($boot -ne '1') { Start-Sleep -Seconds 3 }
}
if ($boot -ne '1') { Write-Warning '系统启动标记未在 180s 内就绪，继续后续步骤（真机已就绪可忽略此提示）' }

# ──────────────── 3/5 构建 APK ────────────────
Write-Host "`n== 3/5 构建 APK ==" -ForegroundColor Cyan
if ($SkipBuild) {
    Write-Host '跳过构建（-SkipBuild）'
} else {
    Push-Location $repoRoot
    try {
        & .\gradlew.bat :androidApp:assembleDebug
        if ($LASTEXITCODE -ne 0) { throw "gradle 构建失败（exit=$LASTEXITCODE）；请检查 JAVA_HOME 与网络镜像" }
    } finally { Pop-Location }
}
if (-not (Test-Path $apk)) {
    throw "找不到 APK 产物：$apk`n（首次请去掉 -SkipBuild 先构建一次）"
}
Write-Host ("APK    : " + (Split-Path $apk -Leaf))

# ──────────────── 4/5 安装 / 覆盖安装 ────────────────
Write-Host "`n== 4/5 安装 APK ==" -ForegroundColor Cyan
foreach ($d in $devs) {
    Write-Host ("→ {0} install -r …" -f $d)
    & $adb -s $d install -r $apk
    if ($LASTEXITCODE -ne 0) { throw "安装到 $d 失败" }
}

# ──────────────── 5/5 启动 app ────────────────
Write-Host "`n== 5/5 启动 app ==" -ForegroundColor Cyan
foreach ($d in $devs) {
    if (-not $NoRestart) {
        # 干净启动：清内存态、重新走启动读盘（UI 改动/数据验证常用）
        & $adb -s $d shell am force-stop $pkg | Out-Null
        & $adb -s $d shell am start -n $activity
    } else {
        # 保留进程，仅带回前台
        & $adb -s $d shell am start -n $activity
    }
}

# ──────────────── 收尾 ────────────────
Write-Host "`n✅ 启动完成：$($devs -join ', ')  →  $activity" -ForegroundColor Green
Write-Host '下一步：'
Write-Host "  · 验证 UI：进 app 右上角 ⚙️ 设置 → 🧹 清空所有数据 → 载入示例数据（一次喂入 50 条、跨三年、含 5 条归档）"
Write-Host "  · 核对落盘：& `"$adb`" shell run-as $pkg ls -la files/"
Write-Host "  · 看前台：  & `"$adb`" shell `"dumpsys window | grep mCurrentFocus`""
Write-Host '  · 再次快速部署：.\scripts\android-quick-start.ps1 -SkipBuild -NoRestart'
