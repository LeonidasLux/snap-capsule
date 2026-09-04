package com.snapcapsule.app

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.snapcapsule.app.adapter.KRColorParserAdapter
import com.snapcapsule.app.adapter.KRFontAdapter
import com.snapcapsule.app.adapter.KRLogAdapter
import com.snapcapsule.app.adapter.KRRouterAdapter
import com.snapcapsule.app.adapter.KRThreadAdapter
import com.snapcapsule.app.adapter.KRUncaughtExceptionHandlerAdapter
import com.tencent.kuikly.core.render.android.adapter.KuiklyRenderAdapterManager
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator

/** 闪念胶囊 Android 壳：单 Activity 承载 Kuikly @Page("home")。 */
class ShellActivity : AppCompatActivity() {

    private lateinit var contextCodeHandler: ContextCodeHandler
    private lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        contextCodeHandler = ContextCodeHandler(this, PAGE_HOME)
        kuiklyRenderViewDelegator = contextCodeHandler.initContextHandler()
        setContentView(R.layout.activity_hr)
        setupAdapterManager()

        val container: ViewGroup = findViewById(R.id.hr_container)
        ShellBridge.onActivityAttached(this)
        contextCodeHandler.openPage(container, PAGE_HOME, pageData())
    }

    private fun pageData(): Map<String, Any> {
        val map = mutableMapOf<String, Any>()
        map["appId"] = 1
        map["sysLang"] = resources.configuration.locale.language
        map["debug"] = if (BuildConfig.DEBUG) 1 else 0
        return map
    }

    private fun setupAdapterManager() {
        if (KuiklyRenderAdapterManager.krLogAdapter == null) KuiklyRenderAdapterManager.krLogAdapter = KRLogAdapter
        if (KuiklyRenderAdapterManager.krRouterAdapter == null) KuiklyRenderAdapterManager.krRouterAdapter = KRRouterAdapter()
        if (KuiklyRenderAdapterManager.krThreadAdapter == null) KuiklyRenderAdapterManager.krThreadAdapter = KRThreadAdapter()
        if (KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter == null) {
            KuiklyRenderAdapterManager.krUncaughtExceptionHandlerAdapter = KRUncaughtExceptionHandlerAdapter
        }
        if (KuiklyRenderAdapterManager.krFontAdapter == null) KuiklyRenderAdapterManager.krFontAdapter = KRFontAdapter
        if (KuiklyRenderAdapterManager.krColorParseAdapter == null) {
            KuiklyRenderAdapterManager.krColorParseAdapter = KRColorParserAdapter()
        }
    }

    override fun onResume() {
        super.onResume()
        kuiklyRenderViewDelegator.onResume()
    }

    override fun onPause() {
        super.onPause()
        kuiklyRenderViewDelegator.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        ShellBridge.onActivityDetached(this)
        kuiklyRenderViewDelegator.onDetach()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
            if (kuiklyRenderViewDelegator.onBackPressed()) return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        ShellBridge.onActivityResult(requestCode, resultCode, data)
        super.onActivityResult(requestCode, resultCode, data)
    }

    private companion object {
        const val PAGE_HOME = "home"
    }
}
