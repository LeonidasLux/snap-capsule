package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
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
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.snapcapsule.data.CapsuleStore
import com.snapcapsule.theme.Palette

/**
 * 回收站覆盖层：从设置进入。被「删除」的胶囊（TRASHED）在此查看/恢复，
 * 左滑可「恢复」或「彻底删除」；顶部可一键清空回收站。
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
            // 点击页面空白处：收起可能处于左滑展开的卡片
            .pointerInput(Unit) {
                detectTapGestures { UiState.swipeOpenId = null }
            }
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
                    .clickable {
                        UiState.swipeOpenId = null
                        UiState.showTrash = false
                    },
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
                        .clickable {
                            UiState.swipeOpenId = null
                            UiState.confirmEmptyTrash = true
                        }
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
                    CapsuleCard(
                        capsule = c,
                        now = now,
                        primaryLabel = "恢复",
                        primaryColor = Palette.life,
                        onPrimary = {
                            CapsuleStore.restore(c.id)
                            UiState.toast("已恢复到当前列表")
                        },
                        secondaryLabel = "彻底删除",
                        secondaryColor = Palette.danger,
                        onSecondary = { UiState.confirmDeleteId = c.id },
                        onOpen = { UiState.detailId = c.id },
                    )
                }
            }
        }
    }
}
