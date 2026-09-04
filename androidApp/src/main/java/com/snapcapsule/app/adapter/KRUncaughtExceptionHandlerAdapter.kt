package com.snapcapsule.app.adapter

import android.util.Log
import com.tencent.kuikly.core.render.android.adapter.IKRUncaughtExceptionHandlerAdapter

object KRUncaughtExceptionHandlerAdapter : IKRUncaughtExceptionHandlerAdapter {
    override fun uncaughtException(throwable: Throwable) {
        Log.e("SnapCapsule", "kuikly uncaught: ${throwable.stackTraceToString()}")
    }
}
