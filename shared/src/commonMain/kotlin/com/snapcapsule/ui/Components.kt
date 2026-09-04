package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
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
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.snapcapsule.model.Cat
import com.snapcapsule.theme.Palette
import com.snapcapsule.theme.Radius

/** 生活/工作 分类选择按钮（编辑器与详情移动分类共用）。 */
@Composable
fun CatButton(
    cat: Cat,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 46.dp,
) {
    val shape = RoundedCornerShape(Radius.btn)
    val bg = if (selected) Palette.catSoft(cat) else Palette.surface
    val fg = if (selected) Palette.catColor(cat) else Palette.muted
    val borderColor = if (selected) Palette.catColor(cat) else Palette.border
    Box(
        modifier = modifier
            .height(height)
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (cat == Cat.WORK) "💼 工作" else "☕ 生活",
            color = fg,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** 一排两个分类按钮，等分。 */
@Composable
fun RowScopeCatPicker(
    current: Cat,
    onPick: (Cat) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth()) {
        CatButton(cat = Cat.LIFE, selected = current == Cat.LIFE, onClick = { onPick(Cat.LIFE) }, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(10.dp))
        CatButton(cat = Cat.WORK, selected = current == Cat.WORK, onClick = { onPick(Cat.WORK) }, modifier = Modifier.weight(1f))
    }
}

/** 顶部筛选 Tab（今天 / 近一周 / 全部 / 归档），选中态为文字变实 + 底部琥珀圆点。 */
@Composable
fun FilterTabBar(
    tabs: List<com.snapcapsule.model.Filter>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        tabs.forEachIndexed { i, f ->
            val selected = i == selectedIndex
            val color = if (selected) Palette.fg else Palette.muted
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = f.label,
                        color = color,
                        fontSize = 15.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    Spacer(Modifier.height(5.dp))
                    Box(
                        Modifier.size(5.dp).clip(RoundedCornerShape(50)).background(
                            if (selected) Palette.accent else Color.Transparent
                        )
                    )
                    Spacer(Modifier.height(5.dp))
                }
            }
        }
    }
}

/** 小分类 pill（详情 / 列表 meta 中）。 */
@Composable
fun CatPill(cat: Cat) {
    val bg = Palette.catSoft(cat)
    val fg = Palette.catColor(cat)
    Box(Modifier.clip(RoundedCornerShape(Radius.chip)).background(bg).padding(horizontal = 10.dp).height(24.dp),
        contentAlignment = Alignment.Center) {
        Text(text = if (cat == Cat.WORK) "💼 工作" else "☕ 生活", color = fg, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** 标签 chip（#开头小字）。 */
@Composable
fun TagChip(tag: String, onRemove: (() -> Unit)? = null, accent: Color = Palette.accent) {
    Row(
        Modifier.clip(RoundedCornerShape(Radius.chip)).background(Palette.accentSoft)
            .padding(start = 10.dp, end = if (onRemove != null) 6.dp else 10.dp).height(24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("#$tag", color = accent, fontSize = 12.sp)
        if (onRemove != null) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(50)).clickable { onRemove() },
                contentAlignment = Alignment.Center,
            ) { Text("✕", color = accent, fontSize = 10.sp) }
        }
    }
}
