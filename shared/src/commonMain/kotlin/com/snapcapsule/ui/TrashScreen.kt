package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.PaddingValues
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
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
import com.snapcapsule.model.Capsule
import com.snapcapsule.model.Cat
import com.snapcapsule.theme.Palette
import com.snapcapsule.theme.Radius
import com.snapcapsule.util.TimeText

private val CAT_META = mapOf(
    Cat.LIFE to "☕ 生活",
    Cat.WORK to "💼 工作",
)

/**
 * 回收站覆盖层：从主页顶栏的回收站图标进入。
 * 条目为按钮式（对齐 v2 回收站）：正文 + meta + 「恢复 / 彻底删除」双按钮，
 * 恢复回到原 Tab（保留完成状态），彻底删除需二次确认。
 */
@Composable
fun TrashScreen() {
    if (!UiState.showTrash) return

    val now = remember { CapsuleStore.now() }
    val items = CapsuleStore.trashed()

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
                    .clickable { UiState.showTrash = false },
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", color = Palette.fg, fontSize = 26.sp)
            }
            Text(
                text = "回收站",
                color = Palette.fg,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.weight(1f),
            )
            if (items.isNotEmpty()) {
                Box(
                    Modifier
                        .padding(end = 8.dp)
                        .clickable { UiState.confirmEmptyTrash = true }
                        .padding(8.dp)
                ) {
                    Text("🗑 清空", color = Palette.danger, fontSize = 14.sp)
                }
            }
        }

        if (items.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.size(84.dp).clip(RoundedCornerShape(50)).background(Palette.fgSoft),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🗑", fontSize = 40.sp)
                }
                Spacer(Modifier.height(18.dp))
                Text("回收站是空的", color = Palette.muted, fontSize = 20.sp, fontFamily = FontFamily.Serif)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = items, key = { it.id }) { c ->
                    TrashItem(c, now)
                }
            }
        }
    }
}

@Composable
private fun TrashItem(capsule: Capsule, now: Long) {
    val shape = RoundedCornerShape(Radius.card)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Palette.surface, shape)
            .padding(start = 15.dp, end = 15.dp, top = 13.dp, bottom = 13.dp)
    ) {
        // 正文（回收站里看全，不截断）
        Text(
            text = capsule.text,
            color = Palette.fg,
            fontSize = 14.sp,
            lineHeight = 21.sp,
        )
        Text(
            text = "${CAT_META[capsule.cat]} · ${TimeText.relative(capsule.createdAt, now)}",
            color = Palette.muted,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 8.dp),
        )
        // 动作：恢复 / 彻底删除（按钮式，无需左滑即可操作）
        Row(Modifier.fillMaxWidth().padding(top = 12.dp)) {
            TrashButton("恢复", Palette.fgSoft, Palette.fg, Modifier.weight(1f)) {
                CapsuleStore.restore(capsule.id)
                UiState.toast("已恢复")
            }
            Spacer(Modifier.width(8.dp))
            TrashButton("彻底删除", Palette.dangerSoft, Palette.danger, Modifier.weight(1f)) {
                UiState.confirmDeleteId = capsule.id
            }
        }
    }
}

@Composable
private fun TrashButton(
    label: String,
    bg: com.tencent.kuikly.compose.ui.graphics.Color,
    fg: com.tencent.kuikly.compose.ui.graphics.Color,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
