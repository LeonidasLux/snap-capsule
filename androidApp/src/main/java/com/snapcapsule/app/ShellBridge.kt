package com.snapcapsule.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.widget.TextView
import android.widget.Toast
import com.snapcapsule.ui.CapBridge
import com.snapcapsule.ui.ImportDispatcher
import com.snapcapsule.ui.ToastPresenter

/**
 * Android 侧平台能力桥：把「导出/导入」接到系统分享面板与文件选择器。
 * 由 ShellApplication.install() 注入到 shared 的 [CapBridge]。
 */
object ShellBridge {
    private const val REQ_IMPORT = 1001

    private var currentActivity: Activity? = null

    private var lastToast: Toast? = null

    fun install() {
        CapBridge.onExportJson = { _fileName, text -> share(currentActivity, text) }
        CapBridge.onPickImportFile = { pick(currentActivity) }
        // Toast 用系统 Toast（非模态、浮在所有弹层之上且不拦截点击）；
        // 若用 Compose Dialog 画提示，全屏模态窗口会让弹出期间整页不可点。
        ToastPresenter.show = { msg -> showToast(msg) }
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

    /** 顶部 Toast（自绘暖黑圆角胶囊，视觉对齐 Compose 版本），用系统 Toast 保证非模态。 */
    private fun showToast(msg: String) {
        val activity = currentActivity ?: return
        Handler(Looper.getMainLooper()).post {
            lastToast?.cancel()
            val toast = Toast(activity.applicationContext)
            val tv = TextView(activity).apply {
                text = msg
                setTextColor(Color.WHITE)
                textSize = 13f
                setPadding(dp(18), dp(11), dp(18), dp(11))
            }
            val bg = GradientDrawable().apply {
                setColor(0xFF2A2620.toInt())
                cornerRadius = dp(12).toFloat()
            }
            tv.background = bg
            toast.view = tv
            toast.setGravity(
                Gravity.TOP or Gravity.CENTER_HORIZONTAL,
                0,
                statusBarHeight(activity) + dp(48),
            )
            toast.duration = Toast.LENGTH_SHORT
            toast.show()
            lastToast = toast
        }
    }

    private fun dp(v: Int): Int {
        val a = currentActivity ?: return v
        val density = a.resources.displayMetrics.density
        return (v * density).toInt()
    }

    private fun statusBarHeight(activity: Activity): Int {
        val id = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) activity.resources.getDimensionPixelSize(id) else 0
    }
}
