package com.snapcapsule.app

import android.content.Context
import android.view.ViewGroup
import com.tencent.kuikly.core.render.android.context.KuiklyRenderCoreExecuteModeBase
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegator
import com.tencent.kuikly.core.render.android.expand.KuiklyRenderViewBaseDelegatorDelegate
import com.tencent.kuikly.core.render.android.exception.ErrorReason

/** 极简宿主：单页 home，JVM 执行模式。 */
open class ContextCodeHandler(
    @Suppress("unused") private val context: Context,
    @Suppress("unused") val pageName: String,
) {
    lateinit var kuiklyRenderViewDelegator: KuiklyRenderViewBaseDelegator

    open fun initContextHandler(): KuiklyRenderViewBaseDelegator {
        val delegate = object : KuiklyRenderViewBaseDelegatorDelegate {
            override fun coreExecuteModeX(): KuiklyRenderCoreExecuteModeBase =
                KuiklyRenderCoreExecuteModeBase.JVM

            override fun registerExternalModule(kuiklyRenderExport: com.tencent.kuikly.core.render.android.IKuiklyRenderExport) {
                super.registerExternalModule(kuiklyRenderExport)
            }

            override fun registerExternalRenderView(kuiklyRenderExport: com.tencent.kuikly.core.render.android.IKuiklyRenderExport) {
                super.registerExternalRenderView(kuiklyRenderExport)
            }

            override fun registerTDFModule(kuiklyRenderExport: com.tencent.kuikly.core.render.android.IKuiklyRenderExport) {
                super.registerTDFModule(kuiklyRenderExport)
            }

            override fun onKuiklyRenderViewCreated() = super.onKuiklyRenderViewCreated()
            override fun onKuiklyRenderContentViewCreated() = super.onKuiklyRenderContentViewCreated()

            override fun onUnhandledException(
                throwable: Throwable,
                errorReason: ErrorReason,
                executeMode: KuiklyRenderCoreExecuteModeBase,
            ) {
                android.util.Log.e("SnapCapsule", "kuikly unhandled: ${throwable.stackTraceToString()}")
            }

            override fun onPageLoadComplete(
                isSucceed: Boolean,
                errorReason: ErrorReason?,
                executeMode: KuiklyRenderCoreExecuteModeBase,
            ) = Unit

            override fun syncSendEvent(event: String): Boolean = false
        }
        kuiklyRenderViewDelegator = KuiklyRenderViewBaseDelegator(delegate)
        return kuiklyRenderViewDelegator
    }

    fun openPage(hrContainerView: ViewGroup, pageName: String, pageData: Map<String, Any>) {
        kuiklyRenderViewDelegator.onAttach(hrContainerView, "", pageName, pageData)
    }
}
