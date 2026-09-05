@file:OptIn(ExperimentalLayoutApi::class)

package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.gestures.detectHorizontalDragGestures
import com.tencent.kuikly.compose.foundation.gestures.detectTapGestures
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ExperimentalLayoutApi
import com.tencent.kuikly.compose.foundation.layout.FlowRow
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxHeight
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.offset
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.input.pointer.pointerInput
import com.tencent.kuikly.compose.ui.platform.LocalDensity
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.style.TextOverflow
import com.tencent.kuikly.compose.ui.unit.IntOffset
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.snapcapsule.model.Capsule
import com.snapcapsule.theme.Palette
import com.snapcapsule.theme.Radius
import com.snapcapsule.util.TimeText
import kotlin.math.roundToInt

/** 右侧动作面板每格宽度(dp)：主动作 + 次动作两格并排、同宽。 */
private const val ACTION_DP = 96f

/**
 * 可滑动胶囊卡片：白色圆角卡片 + 左侧分类色条 + 两行正文 + 换行标签 + 相对时间。
 * 手势：只支持左滑——手指左滑使卡片向左移，露出右侧两格动作面板
 * （primary=主动作「归档/恢复」，secondary=次动作「删除/彻底删除」，由调用场景传参）。
 * 拖过一半自动吸附全展开，点露出的动作区执行；卡片点一下回弹；右滑不展开动作。
 * 展开互斥：同一时刻只允许一条处于左滑展开态（见 UiState.swipeOpenId），再滑别条会自动收起上一条。
 */
@Composable
fun CapsuleCard(
    capsule: Capsule,
    now: Long,
    primaryLabel: String,
    primaryColor: Color,
    onPrimary: () -> Unit,
    secondaryLabel: String = "删除",
    secondaryColor: Color = Palette.danger,
    onSecondary: () -> Unit,
    onOpen: () -> Unit,
) {
    val catColor = Palette.catColor(capsule.cat)
    val shape = RoundedCornerShape(Radius.card)

    // 动作格按 dp 布局，而手势/偏移记录的是物理像素：用当前密度把每格换算成像素宽，
    // 保证「全展开」位移量恰好等于右侧两格实际总宽，两格都能完整露出。
    val revealPx = ACTION_DP * LocalDensity.current.density * 2f

    // 左滑展开互斥：谁被展开由共享 UiState.swipeOpenId 唯一记录。本卡读它：等于自己→全展开，
    // 否则收起为 0。手指拖动期间走本地瞬态位移（跟随手指，收放交给共享 id）。
    var dragging by remember(capsule.id) { mutableStateOf(false) }
    var dragPx by remember(capsule.id) { mutableFloatStateOf(0f) }
    val shownPx = if (dragging) dragPx else if (UiState.swipeOpenId == capsule.id) -revealPx else 0f

    Box(Modifier.fillMaxWidth().clip(shape)) {
        // 底层动作区：尺寸对齐上层卡片。右侧并排两格——主动作贴最右、次动作靠左，
        // 卡片向左滑开后才露出来（右滑不会展开）。
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Transparent)
        ) {
            Row(Modifier.fillMaxSize()) {
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .width(ACTION_DP.dp)
                        .fillMaxHeight()
                        .background(secondaryColor)
                        .clickable {
                            UiState.swipeOpenId = null
                            onSecondary()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(secondaryLabel, color = Palette.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Box(
                    Modifier
                        .width(ACTION_DP.dp)
                        .fillMaxHeight()
                        .background(primaryColor)
                        .clickable {
                            UiState.swipeOpenId = null
                            onPrimary()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(primaryLabel, color = Palette.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 上层卡片
        Row(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(shownPx.roundToInt(), 0) }
                .background(Palette.surface)
                .pointerInput(capsule.id) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            // 起手先收掉别的展开卡；自己若已展开则保持原位继续拖
                            val opened = UiState.swipeOpenId
                            if (opened != null && opened != capsule.id) UiState.swipeOpenId = null
                            dragPx = if (UiState.swipeOpenId == capsule.id) -revealPx else 0f
                            dragging = true
                        },
                        onDragEnd = {
                            dragging = false
                            val open = dragPx <= -revealPx * 0.5f
                            dragPx = 0f
                            UiState.swipeOpenId = if (open) capsule.id else null
                        },
                        onDragCancel = {
                            dragging = false
                            dragPx = 0f
                            UiState.swipeOpenId = null
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            dragPx = (dragPx + dragAmount).coerceIn(-revealPx, 0f)
                        }
                    )
                }
                .pointerInput(capsule.id) {
                    detectTapGestures(
                        onLongPress = { onOpen() },
                        onTap = {
                            // 点自身（展开中）→ 仅收起；点别的卡片 → 先收起展开态再走正常点击
                            if (UiState.swipeOpenId == capsule.id) {
                                UiState.swipeOpenId = null
                            } else {
                                if (UiState.swipeOpenId != null) UiState.swipeOpenId = null
                                onOpen()
                            }
                        }
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .width(3.dp)
                    .padding(vertical = 12.dp)
                    .background(catColor, RoundedCornerShape(2.dp))
            )
            Column(Modifier.weight(1f).padding(end = 16.dp, top = 13.dp, bottom = 12.dp)) {
                Text(
                    text = capsule.text,
                    color = Palette.fg,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (capsule.tags.isNotEmpty()) {
                        Box(Modifier.weight(1f)) {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                capsule.tags.take(4).forEach { tag -> TagChip(tag) }
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = TimeText.relative(capsule.createdAt, now),
                        color = Palette.muted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
    }
}
