package com.snapcapsule.h5app

import com.tencent.kuikly.core.render.web.ktx.SizeI
import kotlinx.browser.document
import kotlinx.browser.window

/**
 * H5 壳入口：读 URL 的 page_name（默认 home），初始化 web 渲染委派并显示。
 */
fun main() {
    val pageName = queryParam("page_name") ?: "home"
    val w = window.innerWidth
    val h = window.innerHeight

    val delegator = KuiklyWebRenderViewDelegator()
    delegator.init(
        containerId = "root",
        pageName = pageName,
        pageData = mapOf(
            "statusBarHeight" to 0,
            "activityWidth" to w,
            "activityHeight" to h,
            "param" to mapOf("is_H5" to "1"),
        ),
        size = SizeI(w, h),
    )
    delegator.resume()

    document.addEventListener("visibilitychange", {
        val hidden = document.asDynamic().hidden as Boolean
        if (hidden) delegator.pause() else delegator.resume()
    })
}

private fun queryParam(name: String): String? {
    val href = window.location.href
    val q = href.indexOf('?')
    if (q < 0) return null
    href.substring(q + 1).split('&').forEach { kv ->
        val i = kv.indexOf('=')
        if (i > 0 && kv.substring(0, i) == name) return kv.substring(i + 1)
    }
    return null
}
