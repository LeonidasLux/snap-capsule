package com.snapcapsule.data

import kotlinx.browser.window

actual object CapsuleFileStorage {

    private const val KEY = "snap_capsules"

    actual fun read(): String? =
        window.localStorage.getItem(KEY)

    actual fun write(text: String): Boolean {
        window.localStorage.setItem(KEY, text)
        return true
    }
}
