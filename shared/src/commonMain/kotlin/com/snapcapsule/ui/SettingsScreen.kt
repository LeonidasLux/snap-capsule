package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.snapcapsule.data.CapsuleStore
import com.snapcapsule.theme.Palette

/** 设置与数据管理（整屏覆盖层）。 */
@Composable
fun SettingsScreen() {
    if (!UiState.showSettings) return

    Column(
        Modifier
            .fillMaxSize()
            .background(Palette.bg)
    ) {
        // 顶栏
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { UiState.showSettings = false },
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", color = Palette.fg, fontSize = 26.sp)
            }
            Text(
                text = "设置与数据管理",
                color = Palette.fg,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
            )
        }

        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 6.dp)) {
            SettingsRow("⬇️  导出数据", right = "分享/下载 JSON") { doExport() }
            SettingsRow("⬆️  导入数据", right = "选择 .json 文件") { doPickImport() }
            SettingsRow("🧹  载入示例数据", right = if (CapsuleStore.totalCount == 0) "当前为空" else "仅空列表可用") {
                if (CapsuleStore.loadSampleIfEmpty()) UiState.toast("已载入示例数据") else UiState.toast("请先清空再载入示例")
            }
            SettingsRow("📊  数据总量", right = "共 ${CapsuleStore.totalCount} 条", clickable = false)
            SettingsRow("🗑  清空所有数据", right = "不可恢复", danger = true) {
                if (CapsuleStore.totalCount == 0) UiState.toast("已没有数据")
                else UiState.confirmClear = true
            }
        }
    }
}

private fun doExport() {
    val json = CapsuleStore.exportJson()
    val handler = CapBridge.onExportJson
    if (handler != null) {
        handler("shannian-capsules.json", json)
    } else {
        UiState.toast("导出功能暂未在此平台就绪")
    }
}

private fun doPickImport() {
    val handler = CapBridge.onPickImportFile
    if (handler != null) {
        handler()
    } else {
        UiState.toast("导入功能暂未在此平台就绪")
    }
}

@Composable
private fun SettingsRow(
    label: String,
    right: String,
    danger: Boolean = false,
    clickable: Boolean = true,
    onClick: () -> Unit = {},
) {
    val color = if (danger) Palette.danger else Palette.fg
    var mod = Modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp)
    if (clickable) mod = mod.clickable { onClick() }
    Row(
        mod.height(52.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = color, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text(right, color = Palette.muted, fontSize = 13.sp)
        Spacer(Modifier.width(6.dp))
        Text("›", color = Palette.muted, fontSize = 15.sp)
    }
}
