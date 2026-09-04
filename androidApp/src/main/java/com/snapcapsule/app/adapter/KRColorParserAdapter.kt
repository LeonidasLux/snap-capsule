package com.snapcapsule.app.adapter

import com.tencent.kuikly.core.render.android.adapter.IKRColorParserAdapter

/** 本 App 不使用 css 颜色 token；仅透传 ARGB 数值字符串。 */
class KRColorParserAdapter : IKRColorParserAdapter {
    override fun toColor(colorStr: String): Int? = colorStr.toLongOrNull()?.toInt()
}
