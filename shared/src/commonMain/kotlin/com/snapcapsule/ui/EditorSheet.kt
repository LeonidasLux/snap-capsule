package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.border
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ExperimentalLayoutApi
import com.tencent.kuikly.compose.foundation.layout.FlowRow
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.size
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.ModalBottomSheet
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.material3.TextField
import com.tencent.kuikly.compose.material3.TextFieldDefaults
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontFamily
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.text.input.TextFieldValue
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.snapcapsule.data.CapsuleStore
import com.snapcapsule.model.Capsule
import com.snapcapsule.model.Cat
import com.snapcapsule.theme.Palette
import com.snapcapsule.theme.Radius
import com.snapcapsule.util.TimeText

private val catKeyLabel = mapOf(Cat.LIFE to "生活", Cat.WORK to "工作")

/**
 * 新建 / 编辑 / 查看 半屏弹窗。对齐 v2-1 原型：
 *  - 新建(new)：标题「新建胶囊」，无「更多」，可编辑。
 *  - 编辑(edit)：标题「编辑胶囊」，正文/分类/标签可改，底部左「删除此条」；含「更多」时间折叠。
 *  - 查看(view)：回收站点条目进入，标题「查看胶囊」，正文/分类/标签只读，无保存/删除此条/标签输入，按钮为「关闭」；含「更多」时间折叠。
 * 布局：把手 → 标题+保存 → 输入区 → 分类 → 标签 → 「更多」时间 → 底部操作。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorSheet() {
    val viewId = UiState.viewingId
    val editId = UiState.editingId
    if (!UiState.editorVisible && viewId == null) return

    val targetId = viewId ?: editId
    val base: Capsule? = targetId?.let { CapsuleStore.find(it) }
    // 目标已被彻底删除等异常：直接关闭
    if (targetId != null && base == null) {
        UiState.editingId = null
        UiState.viewingId = null
        return
    }

    // 只要目标在回收站（或走 viewingId 进入）就强制「查看胶囊」只读态：
    // 即便有入口误设了 editingId，也绝不允许对已删除条目呈现编辑控件（对齐 v2-1 原型）。
    val isView = viewId != null || base?.trashed == true
    val isEdit = !isView && base != null
    val isNew = !isView && base == null

    var textValue by remember(targetId) { mutableStateOf(TextFieldValue(base?.text ?: "")) }
    var cat by remember(targetId) { mutableStateOf(base?.cat ?: Cat.LIFE) }
    var tags by remember(targetId) { mutableStateOf<List<String>>(base?.tags ?: emptyList()) }
    var tagInputValue by remember(targetId) { mutableStateOf(TextFieldValue("")) }
    var metaOpen by remember(targetId) { mutableStateOf(false) }

    fun close() {
        UiState.editorVisible = false
        UiState.editingId = null
        UiState.viewingId = null
    }

    fun addTag() {
        if (isView) return
        val v = tagInputValue.text.trim().removePrefix("#")
        if (v.isNotEmpty() && !tags.contains(v)) tags = tags + v
        tagInputValue = TextFieldValue("")
    }

    fun save() {
        if (isView) return
        val content = textValue.text.trim()
        if (content.isEmpty()) {
            UiState.toast("先记点什么吧")
            return
        }
        val finalTags = if (tags.isNotEmpty()) tags else listOf(catKeyLabel.getValue(cat))
        if (base != null) {
            CapsuleStore.update(base.id, content, cat, finalTags)
            UiState.toast("已更新")
        } else {
            CapsuleStore.add(content, cat, finalTags)
            UiState.toast("已保存")
        }
        close()
    }

    /** 编辑态底部「删除此条」：移入回收站（可恢复），不弹确认（对齐 v2 原型）。 */
    fun delete() {
        val id = UiState.editingId ?: return
        CapsuleStore.trash(id)
        UiState.toast("已移入回收站")
        close()
    }

    val wellShape = RoundedCornerShape(Radius.btn)

    ModalBottomSheet(
        visible = true,
        onDismissRequest = { close() },
        modifier = Modifier,
        containerColor = Palette.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp)) {
            // 顶部居中把手
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clipShape(RoundedCornerShape(2.dp), Palette.border)
                )
            }
            Spacer(Modifier.height(16.dp))

            // 顶栏：标题 +（非查看态）右上角琥珀「保存」
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        isView -> "查看胶囊"
                        base != null -> "编辑胶囊"
                        else -> "新建胶囊"
                    },
                    color = Palette.fg,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.weight(1f),
                )
                if (!isView) {
                    Box(
                        Modifier
                            .clickable { save() }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text("保存", color = Palette.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))

            // 内容区：查看态为只读全文；新建/编辑为大号琥珀浅底圆角输入盒
            if (isView) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clipShape(wellShape, Palette.accentSoft)
                        .border(1.dp, Palette.border, wellShape)
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = base?.text.orEmpty(),
                        color = Palette.fg,
                        fontSize = 15.sp,
                        lineHeight = 23.sp,
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clipShape(wellShape, Palette.accentSoft)
                        .border(1.dp, Palette.border, wellShape)
                ) {
                    TextField(
                        value = textValue,
                        onValueChange = { textValue = it },
                        placeholder = { Text("记个闪念…", color = Palette.muted) },
                        maxLines = 8,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Palette.accent,
                        ),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp),
                    )
                }
            }
            Spacer(Modifier.height(16.dp))

            // 分类（查看态只读：点击无动作，仅保持选中外观）
            RowScopeCatPicker(
                current = cat,
                onPick = { picked -> if (!isView) cat = picked },
            )
            Spacer(Modifier.height(14.dp))

            // 标签输入（仅新建/编辑；查看态隐藏，标签以只读 chip 展示）
            if (!isView) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#", color = Palette.accent, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.Monospace)
                    TextField(
                        value = tagInputValue,
                        onValueChange = { tagInputValue = it },
                        placeholder = { Text("标签，输完点 ＋", color = Palette.muted) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Palette.accent,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        Modifier
                            .size(32.dp)
                            .clipPill(Palette.accentSoft)
                            .clickable { addTag() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("＋", color = Palette.accent, fontSize = 18.sp)
                    }
                }
            }

            // 已添加标签：查看态只读（无 ×）
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        if (isView) TagChip(tag) else TagChip(tag = tag, onRemove = { tags = tags.filterNot { it == tag } }, accent = Palette.accent)
                    }
                }
            }

            // 「更多」时间折叠：仅新建态隐藏；编辑/查看态默认收起
            if (!isNew) {
                val t = base
                Spacer(Modifier.height(10.dp))
                val summary = when {
                    t != null && t.completedAt != null -> "完成于 " + TimeText.relative(t.completedAt, CapsuleStore.now())
                    t != null && t.done -> "已完成"
                    isView -> "查看信息"
                    else -> "未完成"
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { metaOpen = !metaOpen }
                        .padding(vertical = 10.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (metaOpen) "▴" else "▾", color = Palette.muted, fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text("更多", color = if (metaOpen) Palette.fg else Palette.muted, fontSize = 14.sp)
                    Spacer(Modifier.weight(1f))
                    Text(summary, color = Palette.muted, fontSize = 12.sp, maxLines = 1)
                }
                if (metaOpen && t != null) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clipShape(RoundedCornerShape(12.dp), Palette.fgSoft)
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        MetaRow("创建时间", TimeText.meta(t.createdAt))
                        MetaRow("更新时间", TimeText.meta(t.updatedAt))
                        MetaRow("完成时间", t.completedAt?.let { TimeText.meta(it) } ?: "—")
                    }
                }
            }

            // 底部操作行：编辑态左「删除此条」，右侧「取消/关闭」（查看态无删除）
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                if (isEdit) {
                    Box(
                        Modifier
                            .clickable { delete() }
                            .padding(top = 6.dp, bottom = 2.dp, start = 2.dp, end = 12.dp)
                    ) {
                        Text("删除此条", color = Palette.danger, fontSize = 14.sp)
                    }
                }
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .clickable { close() }
                        .padding(top = 6.dp, bottom = 2.dp, start = 12.dp, end = 2.dp)
                ) {
                    Text(if (isView) "关闭" else "取消", color = Palette.muted, fontSize = 14.sp)
                }
            }
        }
    }
}

/** 「更多」里的一行：灰 label + 等宽 value。 */
@Composable
private fun MetaRow(key: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(key, color = Palette.muted, fontSize = 13.sp, modifier = Modifier.width(72.dp))
        Text(value, color = Palette.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
    }
    Spacer(Modifier.height(2.dp))
}

/** 用给定颜色填充并 clip 成圆角。 */
private fun Modifier.clipShape(shape: RoundedCornerShape, color: Color): Modifier = this.then(
    Modifier.background(color, shape)
)

private fun Modifier.clipPill(color: Color): Modifier = this.then(
    Modifier.background(color, RoundedCornerShape(50))
)
