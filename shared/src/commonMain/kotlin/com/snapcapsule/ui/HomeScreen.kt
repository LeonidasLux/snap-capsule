package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.tencent.kuikly.compose.foundation.layout.width
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
import com.tencent.kuikly.compose.ui.zIndex
import com.snapcapsule.data.CapsuleStore
import com.snapcapsule.model.Filter
import com.snapcapsule.platform.BuildInfo
import com.snapcapsule.theme.Palette
import com.snapcapsule.util.TimeText

private val ALL_FILTERS = Filter.entries.toList()

/** 主页：标题栏（回收站 + 设置）+ 未完成/已完成筛选 + 胶囊列表/空态 + 新建 FAB。 */
@Composable
fun HomeScreen() {
    val now = remember { CapsuleStore.now() }

    Column(
        Modifier
            .fillMaxSize()
            // 点击页面空白处：收起可能处于左滑展开的卡片（有按钮/卡片的点击会被其自身消费，不影响）
            .pointerInput(Unit) {
                detectTapGestures { UiState.swipeOpenId = null }
            }
    ) {
        // 标题栏：左标题 + 右「回收站 / 设置」（对齐 v2 顶栏）
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 10.dp, top = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "闪念胶囊",
                color = Palette.fg,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Serif,
                modifier = Modifier.weight(1f),
            )
            // 回收站直达（v2：独立于设置的顶栏入口）
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        UiState.swipeOpenId = null
                        UiState.showTrash = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("🗑", fontSize = 19.sp)
            }
            // 设置
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        UiState.swipeOpenId = null
                        UiState.showSettings = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("⚙️", fontSize = 19.sp)
            }
        }

        // 仅开发/调试构建：标题下方显示本次构建时刻（辅助确认装到的是最新产物）
        if (CapBridge.isDebugBuild) {
            Text(
                text = "开发构建 ${TimeText.stamp(BuildInfo.BUILD_EPOCH_MS)}",
                color = Palette.muted,
                fontSize = 10.sp,
                modifier = Modifier.padding(start = 22.dp, bottom = 6.dp),
            )
        }

        // 筛选：未完成 / 已完成
        val currentIndex = remember(CapsuleStore.filter) { ALL_FILTERS.indexOf(CapsuleStore.filter).coerceAtLeast(0) }
        FilterTabBar(
            tabs = ALL_FILTERS,
            selectedIndex = currentIndex,
            onSelect = { i ->
                CapsuleStore.applyFilter(ALL_FILTERS[i])
                UiState.swipeOpenId = null
            },
        )

        // 列表区
        Box(Modifier.weight(1f)) {
            val items = CapsuleStore.visible()
            Column(Modifier.fillMaxSize()) {
                if (items.isNotEmpty()) {
                    ListHintRow()
                }
                if (items.isEmpty()) {
                    EmptyState(filter = CapsuleStore.filter)
                } else {
                    val filterNow = CapsuleStore.filter
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 2.dp, bottom = 132.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(items = items, key = { it.id }) { c ->
                            CapsuleCard(
                                capsule = c,
                                now = now,
                                // 当前 Tab 内 done 状态一致：未完成 Tab → 主操作“完成”，已完成 Tab → “移回未完成”
                                primaryLabel = if (filterNow == Filter.OPEN) "完成" else "未完成",
                                primaryColor = Palette.life,
                                onPrimary = {
                                    val toDone = filterNow == Filter.OPEN
                                    CapsuleStore.setDone(c.id, toDone)
                                    UiState.toast(if (toDone) "已标记完成" else "已移回未完成")
                                },
                                onSecondary = { CapsuleStore.trash(c.id); UiState.toast("已移入回收站") },
                                // 对齐 v2：点卡片直接进可编辑抽屉（查看 + 编辑一体）
                                onOpen = {
                                    UiState.swipeOpenId = null
                                    UiState.viewingId = null
                                    UiState.editingId = c.id
                                    UiState.editorVisible = true
                                },
                            )
                        }
                    }
                }
            }

            // 新建 FAB（居中底部）—— 显式提升 zIndex，恒浮在列表数据之上
            Box(
                Modifier
                    .zIndex(1f)
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp)
                    .size(56.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Palette.accent)
                    .clickable {
                        UiState.viewingId = null
                        UiState.editingId = null
                        UiState.editorVisible = true
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text("＋", color = Palette.onAccent, fontSize = 28.sp, fontWeight = FontWeight.Light)
            }
        }
    }
}

/** 列表顶部的一次性提示（可关闭，会话内记住）。 */
@Composable
private fun ListHintRow() {
    var show by remember { mutableStateOf(true) }
    if (!show) return
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "点卡片查看/编辑 · 左滑完成/删除",
            color = Palette.muted,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                .size(26.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable { show = false },
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", color = Palette.muted, fontSize = 12.sp)
        }
    }
}

/** 空状态。 */
@Composable
private fun EmptyState(filter: Filter) {
    val title = when (filter) {
        Filter.OPEN -> "此刻大脑很干净"
        Filter.DONE -> "已完成里空空的"
    }
    Column(
        Modifier.fillMaxSize().padding(bottom = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(50)).background(Palette.fgSoft),
            contentAlignment = Alignment.Center,
        ) {
            Text("💡", fontSize = 40.sp)
        }
        Spacer(Modifier.height(18.dp))
        Text(title, color = Palette.muted, fontSize = 20.sp, fontFamily = FontFamily.Serif)
    }
}
