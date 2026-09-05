package com.snapcapsule.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.snapcapsule.model.Capsule

/** 顶层 UI 状态（单页状态机的各处开关与确认位）。 */
object UiState {
    // 设置
    var showSettings by mutableStateOf(false)

    // 回收站整屏覆盖层
    var showTrash by mutableStateOf(false)

    // 新建/编辑
    var editorVisible by mutableStateOf(false)
    var editingId by mutableStateOf<Long?>(null)

    // 详情
    var detailId by mutableStateOf<Long?>(null)

    // 左滑展开互斥：同一时刻只允许一条胶囊处于左滑展开态，记录其 id（null=无展开）。
    var swipeOpenId by mutableStateOf<Long?>(null)

    // 彻底删除确认（回收站内单条）
    var confirmDeleteId by mutableStateOf<Long?>(null)

    // 清空回收站确认
    var confirmEmptyTrash by mutableStateOf(false)

    // 清空全部确认
    var confirmClear by mutableStateOf(false)

    // Toast
    var toastText by mutableStateOf<String?>(null)
    var toastSeq by mutableStateOf(0)

    fun toast(msg: String) {
        toastText = msg
        toastSeq++
    }
}

/** 导入等待队列：平台（选文件）读到的文本先放这，UI 统一消费。 */
object ImportDispatcher {
    var pendingText by mutableStateOf<String?>(null)
}

/** 平台能力桥：由 App 在装配阶段挂上真正实现（H5/Android）。 */
object CapBridge {
    /** 导出 json：H5=下载，Android=系统分享。未装配时按钮给出提示。 */
    var onExportJson: ((String, String) -> Unit)? = null

    /** 唤起“选择文件导入”。 */
    var onPickImportFile: (() -> Unit)? = null
}

/** 导入预览（供确认对话框）。 */
data class ImportOffer(val capsules: List<Capsule>)

/** 导入确认对话框状态。 */
object ImportUi {
    var offer by mutableStateOf<ImportOffer?>(null)
}
