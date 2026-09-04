package com.snapcapsule

import android.content.Context

/** 由 androidApp 壳工程在 Application.onCreate 注入，供 shared 的 androidMain 使用。 */
object AndroidAppContext {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): Context? = appContext
}
