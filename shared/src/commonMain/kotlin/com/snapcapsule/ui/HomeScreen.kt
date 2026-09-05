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
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.lazy.LazyColumn
import com.tencent.kuikly.compose.foundation.lazy.items
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.zIndex
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.snapcapsule.data.CapsuleStore
import com.snapcapsule.model.Capsule
import com.snapcapsule.model.Filter
import com.snapcapsule.theme.Palette
import com.snapcapsule.theme.Radius

private val ALL_FILTERS = Filter.entries.toList()

/** 主页：标题栏 + 筛选 + 胶囊列表/空态 + 新建 FAB。 */
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
        // 标题栏
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
                Text("⚙️", fontSize = 20.sp)
            }
        }

        // 筛选
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
            if (items.isEmpty()) {
                EmptyState(filter = CapsuleStore.filter)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 132.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = items, key = { it.id }) { c ->
                        val archived = c.status == com.snapcapsule.model.Status.ARCHIVED
                        CapsuleCard(
                            capsule = c,
                            now = now,
                            primaryLabel = if (archived) "恢复" else "归档",
                            primaryColor = Palette.life,
                            onPrimary = {
                                if (archived) {
                                    CapsuleStore.restore(c.id)
                                    UiState.toast("已恢复到当前列表")
                                } else {
                                    CapsuleStore.archive(c.id)
                                    UiState.toast("已归档")
                                }
                            },
                            onSecondary = { CapsuleStore.trash(c.id); UiState.toast("已移入回收站") },
                            onOpen = { UiState.detailId = c.id },
                        )
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

/** 空状态。 */
@Composable
private fun EmptyState(filter: Filter) {
    val title = when (filter) {
        Filter.ARCHIVED -> "归档箱是空的"
        Filter.TODAY -> "今天还没有闪念"
        Filter.WEEK -> "近一周没有闪念"
        Filter.ALL -> "此刻大脑很干净"
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
