package com.snapcapsule.app

import android.app.Activity
import android.content.Intent
import com.snapcapsule.ui.CapBridge
import com.snapcapsule.ui.ImportDispatcher

/**
 * Android 侧平台能力桥：把「导出/导入」接到系统分享面板与文件选择器。
 * 由 ShellApplication.install() 注入到 shared 的 [CapBridge]。
 */
object ShellBridge {
    private const val REQ_IMPORT = 1001

    private var currentActivity: Activity? = null

    fun install() {
        CapBridge.onExportJson = { _fileName, text -> share(currentActivity, text) }
        CapBridge.onPickImportFile = { pick(currentActivity) }
    }

    /** ShellActivity 生命周期挂钩。 */
    fun onActivityAttached(activity: Activity) {
        currentActivity = activity
    }

    fun onActivityDetached(activity: Activity) {
        if (currentActivity === activity) currentActivity = null
    }

    /** ShellActivity.onActivityResult 挂钩：读回所选 JSON 文本。 */
    fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQ_IMPORT && resultCode == Activity.RESULT_OK) {
            val uri = data?.data ?: return
            val text = try {
                currentActivity?.contentResolver?.openInputStream(uri)
                    ?.bufferedReader()?.use { it.readText() }
            } catch (t: Throwable) {
                null
            }
            if (!text.isNullOrBlank()) ImportDispatcher.pendingText = text
        }
    }

    private fun share(activity: Activity?, text: String) {
        val a = activity ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        a.startActivity(Intent.createChooser(send, "导出闪念数据（JSON）"))
    }

    private fun pick(activity: Activity?) {
        val a = activity ?: return
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*"
        }
        try {
            a.startActivityForResult(intent, REQ_IMPORT)
        } catch (t: Throwable) {
            // 无可用文件选择器时静默（UI 不会因此崩溃）
        }
    }
}
