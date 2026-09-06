package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.ui.Modifier
import com.snapcapsule.data.CapsuleStore
import com.snapcapsule.data.DecodeResult
import com.snapcapsule.platform.installPlatformHandlers
import com.snapcapsule.theme.Palette

/** 顶层装配：主页 + 各覆盖层/弹层 + 数据装载 + 导入统一消费。 */
@Composable
fun AppShell() {
    LaunchedEffect(Unit) {
        installPlatformHandlers()
        CapsuleStore.ensureLoaded()
    }

    // 打开任一覆盖层/弹层时，先收起可能处于左滑展开的卡片（点别处应收起展开）
    LaunchedEffect(UiState.showSettings, UiState.showTrash, UiState.editorVisible) {
        if (UiState.showSettings || UiState.showTrash || UiState.editorVisible) {
            UiState.swipeOpenId = null
        }
    }

    // 导入文件文本到达：解析 → 成功弹确认 / 失败 Toast
    LaunchedEffect(ImportDispatcher.pendingText) {
        val pending = ImportDispatcher.pendingText ?: return@LaunchedEffect
        ImportDispatcher.pendingText = null
        when (val r = CapsuleStore.importText(pending)) {
            is DecodeResult.Ok -> ImportUi.offer = ImportOffer(r.capsules)
            is DecodeResult.Err -> UiState.toast(r.reason)
        }
    }

    Box(Modifier.fillMaxSize().background(Palette.bg)) {
        HomeScreen()
        if (UiState.showSettings) SettingsScreen()
        if (UiState.showTrash) TrashScreen()
        EditorSheet()
        DeleteConfirmDialog()
        EmptyTrashConfirmDialog()
        ClearAllConfirmDialog()
        ImportOfferDialog()
    }
}
