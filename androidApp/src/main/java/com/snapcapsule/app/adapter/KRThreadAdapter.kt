package com.snapcapsule.app.adapter

import com.tencent.kuikly.core.render.android.adapter.IKRThreadAdapter
import java.util.concurrent.Executors

private val subThreadPoolExecutor by lazy { Executors.newFixedThreadPool(2) }

class KRThreadAdapter : IKRThreadAdapter {
    override fun executeOnSubThread(task: () -> Unit) {
        subThreadPoolExecutor.execute(task)
    }
}
