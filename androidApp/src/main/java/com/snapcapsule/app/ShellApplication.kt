package com.snapcapsule.app

import android.app.Application
import com.snapcapsule.AndroidAppContext

class ShellApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 供 shared(androidMain) 读写 filesDir/capsules.json
        AndroidAppContext.init(this)
        // 注入导出/导入平台处理器（系统分享面板 / 文件选择器）
        ShellBridge.install()
    }
}
