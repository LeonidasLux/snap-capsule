package com.snapcapsule.app.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.snapcapsule.app.ShellActivity
import com.tencent.kuikly.core.render.android.adapter.IKRRouterAdapter
import org.json.JSONObject

class KRRouterAdapter : IKRRouterAdapter {
    override fun openPage(context: Context, pageName: String, pageData: JSONObject) {
        // 仅单页应用：任何内部跳转都回到主页面
        val starter = Intent(context, ShellActivity::class.java)
        context.startActivity(starter)
    }

    override fun closePage(context: Context) {
        (context as? Activity)?.finish()
    }
}
