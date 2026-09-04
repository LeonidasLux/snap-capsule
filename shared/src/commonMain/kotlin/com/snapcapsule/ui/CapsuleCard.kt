@file:OptIn(ExperimentalLayoutApi::class)

package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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

private const val ACTION_PX = 96f

/**
 * 可滑动胶囊卡片：白色圆角卡片 + 左侧分类色条 + 两行正文 + 换行标签 + 相对时间。
 * 手势：单击/长按→详情；左滑→露出右侧动作（active=删除）；右滑→露出左侧动作（active=归档，archived=恢复）。
 * 拖到位松手吸附，点露出的动作区执行；卡片点一下回弹。
 */
@Composable
fun CapsuleCard(
    capsule: Capsule,
    now: Long,
    primaryLabel: String,
    primaryColor: Color,
    onPrimary: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit,
) {
    val catColor = Palette.catColor(capsule.cat)
    val shape = RoundedCornerShape(Radius.card)
    var offsetPx by remember(capsule.id) { mutableFloatStateOf(0f) }

    fun snap() {
        offsetPx = when {
            offsetPx <= -ACTION_PX * 0.55f -> -ACTION_PX
            offsetPx >= ACTION_PX * 0.55f -> ACTION_PX
            else -> 0f
        }
    }

    Box(Modifier.fillMaxWidth().clip(shape)) {
        // 底层动作区：尺寸对齐上层卡片
        Box(
            Modifier
                .matchParentSize()
                .background(Color.Transparent)
        ) {
            Row(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .width(ACTION_PX.dp)
                        .fillMaxHeight()
                        .background(primaryColor)
                        .clickable { onPrimary() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(primaryLabel, color = Palette.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .width(ACTION_PX.dp)
                        .fillMaxHeight()
                        .background(Palette.danger)
                        .clickable { onDelete() },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("删除", color = Palette.onAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // 上层卡片
        Row(
            Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.roundToInt(), 0) }
                .background(Palette.surface)
                .pointerInput(capsule.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = { snap() },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            offsetPx = (offsetPx + dragAmount).coerceIn(-ACTION_PX, ACTION_PX)
                        }
                    )
                }
                .pointerInput(capsule.id) {
                    detectTapGestures(
                        onLongPress = { onOpen() },
                        onTap = { if (offsetPx != 0f) offsetPx = 0f else onOpen() }
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
