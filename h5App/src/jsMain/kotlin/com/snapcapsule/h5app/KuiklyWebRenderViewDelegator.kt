package com.snapcapsule.h5app

import com.tencent.kuikly.core.render.web.IKuiklyRenderExport
import com.tencent.kuikly.core.render.web.expand.KuiklyRenderViewDelegatorDelegate
import com.tencent.kuikly.core.render.web.ktx.SizeI
import com.tencent.kuikly.core.render.web.runtime.web.expand.KuiklyRenderViewDelegator

/**
 * H5 Web 渲染委派：实现官方 delegate 接口，负责创建/注册各模块与渲染视图。
 */
class KuiklyWebRenderViewDelegator : KuiklyRenderViewDelegatorDelegate {

    private val delegate = KuiklyRenderViewDelegator(this)

    fun init(containerId: String, pageName: String, pageData: Map<String, Any>, size: SizeI) {
        delegate.onAttach(containerId, pageName, pageData, size)
    }

    fun resume() = delegate.onResume()
    fun pause() = delegate.onPause()
    fun detach() = delegate.onDetach()

    /** 注册平台能力模块（导出/导入等能力由业务 jsMain 的 installPlatformHandlers 直接实现）。 */
    override fun registerExternalModule(kuiklyRenderExport: IKuiklyRenderExport) {
        super.registerExternalModule(kuiklyRenderExport)
    }
}
