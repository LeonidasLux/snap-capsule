package com.snapcapsule

import com.tencent.kuikly.compose.ComposeContainer
import com.tencent.kuikly.compose.setContent
import com.tencent.kuikly.core.annotations.Page
import com.snapcapsule.ui.AppShell

/**
 * 闪念胶囊 · 唯一页面入口。
 * 全 App 以单页状态机驱动（主页 / 弹层 / 设置均为同一页面内的 Compose 状态）。
 */
@Page("home")
class HomePager : ComposeContainer() {

    override fun willInit() {
        super.willInit()
        setContent {
            AppShell()
        }
    }
}
