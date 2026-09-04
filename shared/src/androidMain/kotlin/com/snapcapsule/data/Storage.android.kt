package com.snapcapsule.data

import com.snapcapsule.AndroidAppContext
import java.io.File

actual object CapsuleFileStorage {

    private const val FILE_NAME = "capsules.json"
    private const val TMP_NAME = "capsules.json.tmp"

    private fun fileOrNull(): File? {
        val ctx = AndroidAppContext.get() ?: return null
        return File(ctx.filesDir, FILE_NAME)
    }

    actual fun read(): String? {
        val file = fileOrNull() ?: return null
        return if (file.exists()) runCatching { file.readText() }.getOrNull() else null
    }

    actual fun write(text: String): Boolean {
        val ctx = AndroidAppContext.get() ?: return false
        return try {
            val dir = ctx.filesDir
            val target = File(dir, FILE_NAME)
            val tmp = File(dir, TMP_NAME)
            tmp.writeText(text)
            if (target.exists()) target.delete()
            tmp.renameTo(target)
            true
        } catch (t: Throwable) {
            false
        }
    }
}
