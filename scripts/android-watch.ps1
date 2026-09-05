#requires -Version 5.1
<#
  ─────────────────────────────────────────────────────────────
  闪念胶囊 · Android 持续部署脚本（Windows / PowerShell）
  ─────────────────────────────────────────────────────────────
  在 android-quick-start.ps1 之上叠加「持续构建 + 自动重装」的开发循环：

    1. 确保模拟器 / 真机在线（逻辑与 android-quick-start 相同）
    2. 以 gradle --continuous 常驻监听 shared / androidApp 源码
    3. 每轮构建 BUILD SUCCESSFUL 后自动 adb install -r 并拉 app 前台

  用法（开发循环入口，跑一次后一直挂着，Ctrl+C 结束）：
    .\scripts\android-watch.ps1               # 无在线设备时自动启动 AVD [Medium_Phone]
    .\scripts\android-watch.ps1 -NoEmulator   # 不开模拟器，要求已有在线设备 / 真机
    .\scripts\android-watch.ps1 -Avd Pixel_2  # 指定 AVD 名

  之后保存 shared(commonMain/androidMain) 或 androidApp 的源码即自动
  「编译 → 装包 → 重启 app」，无需手动重跑脚本。
  注意：watch 只覆盖 android 目标；shared/jsMain 与 h5App 侧请走 H5 流程。

  Ctrl+C 结束 watch。gradle daemon 保留驻留以加速下次构建；
  想清空可另行执行 .\gradlew.bat --stop。
#>
[CmdletBinding()]
param(
    [switch]$NoEmulator,   # 不自动启动模拟器，要求已有在线设备
    [string]$Avd = 'Medium_Phone'
)

$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$gradlew  = Join-Path $repoRoot 'gradlew.bat'
$apk      = Join-Path $repoRoot 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'
$pkg      = 'com.snapcapsule.app'
$activity = "$pkg/.ShellActivity"

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

# ──────────────── 1/2 检查在线设备，必要时拉起模拟器 ────────────────
Write-Host "`n== 1/2 检查在线设备 ==" -ForegroundColor Cyan
$devs = @(Get-OnlineDevices)
if ($devs.Count -gt 0) {
    Write-Host ("在线设备：" + ($devs -join ', '))
} elseif ($NoEmulator) {
    throw '无在线设备且已指定 -NoEmulator：请先手动启动模拟器或连接真机'
} else {
    if (-not (Test-Path $emu)) { throw "找不到 emulator：$emu" }
    $avds = @(& $emu -list-avds)
    if ($Avd -notin $avds) { throw "AVD [$Avd] 不存在；可用：$($avds -join ', ')" }
    Write-Host "无在线设备，启动 AVD [$Avd]（模拟器窗口将弹出，冷启动引导会慢一些）…"
    Start-Process -FilePath $emu -ArgumentList @('-avd', $Avd, '-no-snapshot-load', '-no-snapshot-save', '-no-boot-anim')
    Write-Host '等待设备连接…'
    & $adb wait-for-device
    Start-Sleep -Seconds 2
    $devs = @(Get-OnlineDevices)
    if ($devs.Count -eq 0) { throw '设备已连上但状态非 device（可能是 offline/unauthorized），请检查模拟器/开发者授权' }
    Write-Host ("设备就绪：" + ($devs -join ', '))
}

# ──────────────── 等待系统启动完成（冷启动模拟器专用，真机秒过） ────────────────
$deadline = (Get-Date).AddSeconds(180)
$boot = ''
while ((Get-Date) -lt $deadline -and $boot -ne '1') {
    $boot = (& $adb shell getprop sys.boot_completed 2>$null | Out-String).Trim()
    if ($boot -ne '1') { Start-Sleep -Seconds 3 }
}
if ($boot -ne '1') { Write-Warning '系统启动标记未在 180s 内就绪，继续后续步骤（真机已就绪可忽略此提示）' }

# ──────────────── 2/2 持续构建 + 每次成功自动部署 ────────────────
function Deploy-To-Devices {
    # 本轮构建已成功：把新 APK 装到所有在线设备并拉 app 前台
    $online = @(Get-OnlineDevices)
    if ($online.Count -eq 0) {
        Write-Warning '无在线设备，跳过本轮安装（设备回来后再保存一次源码即可自动补装）'
        return
    }
    # 构建刚结束 daemon 可能紧接着开下一轮并重写 APK，稍稳一下再装，失败再重试一次
    Start-Sleep -Milliseconds 400
    foreach ($d in $online) {
        Write-Host ("→ {0} install -r …" -f $d) -ForegroundColor Yellow
        & $adb -s $d install -r $apk
        if ($LASTEXITCODE -ne 0) {
            Write-Warning "安装到 $d 失败，稍候重试一次…"
            Start-Sleep -Milliseconds 600
            & $adb -s $d install -r $apk
            if ($LASTEXITCODE -ne 0) { Write-Warning "重试仍失败，跳过启动（等下一轮构建自动再装）"; continue }
        }
        & $adb -s $d shell am start -n $activity | Out-Null
        Write-Host ("已拉起前台：{0}" -f $activity) -ForegroundColor Green
    }
}

Write-Host "`n== 2/2 开始持续监听 shared / androidApp 源码 ==" -ForegroundColor Cyan
Write-Host '  · 首次会先完整构建一轮并自动安装'
Write-Host '  · 之后保存源码即自动「编译 → 装包 → 重启 app」'
Write-Host '  · Ctrl+C 结束 watch'
Write-Host ("`n日志（gradle --continuous，--console=plain）…`n")

$first = $true
& $gradlew -t --console=plain :androidApp:assembleDebug | ForEach-Object {
    $line = $_
    Write-Host $line
    if ($line -match 'BUILD FAILED') {
        Write-Host '  ⚠️ 本轮构建失败，跳过安装。修正源码保存后将自动重试。' -ForegroundColor Red
    } elseif ($line -match 'BUILD SUCCESSFUL') {
        if ($first) {
            Write-Host ("`n首轮构建成功 → 自动安装并启动…") -ForegroundColor Green
            $first = $false
        } else {
            Write-Host ("`n[ {0} ] 检测到源码变更，新一轮构建成功 → 自动安装…" -f (Get-Date -Format 'HH:mm:ss')) -ForegroundColor Green
        }
        Deploy-To-Devices
    }
}

Write-Host "`n⏹ watch 已结束（gradle daemon 保留驻留，如需清空执行 .\gradlew.bat --stop）"
