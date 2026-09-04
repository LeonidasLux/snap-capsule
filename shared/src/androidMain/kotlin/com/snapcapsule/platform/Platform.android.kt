package com.snapcapsule.platform

/**
 * Android 端由 androidApp 壳在拿到 Context/Activity 后直接向 [com.snapcapsule.ui.CapBridge]
 * 注入导出/导入处理器（系统分享面板 / 文件选择器），因此本 actual 为空。
 */
actual fun installPlatformHandlers() {
    // no-op：壳注入见 androidApp 的 Shell 装配
}
