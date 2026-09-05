package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ExperimentalLayoutApi
import com.tencent.kuikly.compose.foundation.layout.FlowRow
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.material3.ModalBottomSheet
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.snapcapsule.data.CapsuleStore
import com.snapcapsule.model.Capsule
import com.snapcapsule.model.Cat
import com.snapcapsule.model.Status
import com.snapcapsule.theme.Palette
import com.snapcapsule.util.TimeText

/** 长按/点按胶囊后的详情面板：全文 + 分类/标签 + 操作。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DetailSheet() {
    val id = UiState.detailId
    if (id == null) return
    val c = CapsuleStore.find(id)
    if (c == null) {
        UiState.detailId = null
        return
    }

    var showCat by remember { mutableStateOf(false) }

    ModalBottomSheet(
        visible = true,
        onDismissRequest = { UiState.detailId = null },
        containerColor = Palette.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 26.dp)) {
            Text(
                text = "胶囊详情",
                color = Palette.fg,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
            )

            // 全文
            Text(
                text = c.text,
                color = Palette.fg,
                fontSize = 15.sp,
                lineHeight = 23.sp,
                modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
            )

            // meta
            Row(verticalAlignment = Alignment.CenterVertically) {
                CatPill(c.cat)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = TimeText.relative(c.createdAt, CapsuleStore.now()),
                    color = Palette.muted,
                    fontSize = 12.sp,
                )
            }
            if (c.tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    c.tags.forEach { tag -> TagChip(tag) }
                }
            }

            // 移动分类（展开）
            if (showCat) {
                Spacer(Modifier.height(14.dp))
                RowScopeCatPicker(
                    current = c.cat,
                    onPick = { newCat ->
                        CapsuleStore.updateCat(c.id, newCat)
                        UiState.toast("已移动分类")
                        UiState.detailId = null
                    },
                )
            }
            Spacer(Modifier.height(8.dp))

            // 操作行：回收站条目只给恢复/彻底删除；主列表与归档箱给编辑/分类/归档与移入回收站
            if (c.status == Status.TRASHED) {
                DetailRow("↩️  恢复到主列表", color = Palette.life) {
                    CapsuleStore.restore(c.id)
                    UiState.toast("已恢复到当前列表")
                    UiState.detailId = null
                }
                DetailRow("🗑  彻底删除", color = Palette.danger) {
                    UiState.detailId = null
                    UiState.confirmDeleteId = c.id
                }
            } else {
                DetailRow("✏️  编辑内容") {
                    UiState.detailId = null
                    UiState.editingId = c.id
                    UiState.editorVisible = true
                }
                DetailRow("📂  移动分类") { showCat = !showCat }
                if (c.status == Status.ARCHIVED) {
                    DetailRow("↩️  恢复到当前列表", color = Palette.life) {
                        CapsuleStore.restore(c.id)
                        UiState.toast("已恢复")
                        UiState.detailId = null
                    }
                }
                DetailRow("🗑  移入回收站", color = Palette.danger) {
                    CapsuleStore.trash(c.id)
                    UiState.toast("已移入回收站")
                    UiState.detailId = null
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    color: Color = Palette.fg,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = color, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Text("›", color = Palette.muted, fontSize = 16.sp)
    }
}
