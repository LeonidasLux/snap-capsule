package com.snapcapsule.platform

import com.snapcapsule.ui.CapBridge

/**
 * 各端安装平台能力（导出下载/选文件导入）到 [CapBridge]。
 * - JS (H5)：在业务 jsMain 内直接安装 DOM 实现；
 * - Android：由 androidApp 壳在拿到 Context/Activity 后注入到 [CapBridge]；
 * - iOS / 鸿蒙：随目标启用后于对应壳中注入（见 docs/PLATFORMS.md）。
 */
expect fun installPlatformHandlers()
