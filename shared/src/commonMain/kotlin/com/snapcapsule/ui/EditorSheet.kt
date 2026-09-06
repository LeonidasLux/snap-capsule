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
import com.snapcapsule.model.Cat
import com.snapcapsule.theme.Palette
import com.snapcapsule.theme.Radius

private val catKeyLabel = mapOf(Cat.LIFE to "生活", Cat.WORK to "工作")

/** 新建 / 编辑半屏弹窗。布局对齐 HTML 原型：顶部把手 → 标题/保存 → 大输入区 → 分类 → 标签 → 取消。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorSheet() {
    if (!UiState.editorVisible) return

    val editingId = UiState.editingId
    val editing = editingId?.let { CapsuleStore.find(it) }
    val isEdit = editing != null

    var textValue by remember { mutableStateOf(TextFieldValue(editing?.text ?: "")) }
    var cat by remember { mutableStateOf(editing?.cat ?: Cat.LIFE) }
    var tags by remember { mutableStateOf<List<String>>(editing?.tags ?: emptyList()) }
    var tagInputValue by remember { mutableStateOf(TextFieldValue("")) }

    fun close() {
        UiState.editorVisible = false
        UiState.editingId = null
    }

    fun addTag() {
        val v = tagInputValue.text.trim().removePrefix("#")
        if (v.isNotEmpty() && !tags.contains(v)) tags = tags + v
        tagInputValue = TextFieldValue("")
    }

    fun save() {
        val content = textValue.text.trim()
        if (content.isEmpty()) {
            UiState.toast("先记点什么吧")
            return
        }
        val finalTags = if (tags.isNotEmpty()) tags else listOf(catKeyLabel.getValue(cat))
        if (isEdit) {
            CapsuleStore.update(editing.id, content, cat, finalTags)
            UiState.toast("已更新")
        } else {
            CapsuleStore.add(content, cat, finalTags)
            UiState.toast("已保存")
        }
        UiState.editorVisible = false
        UiState.editingId = null
    }

    /** 编辑态底部「删除此条」：移入回收站（可恢复），不弹确认（对齐 v2）。 */
    fun delete() {
        val id = editing?.id ?: return
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

            // 顶栏：标题 + 右上角琥珀色文字「保存」（原型 save-btn 无底色）
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (isEdit) "编辑胶囊" else "新建胶囊",
                    color = Palette.fg,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Serif,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    Modifier
                        .clickable { save() }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Text("保存", color = Palette.accent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(16.dp))

            // 内容输入区：大号琥珀浅底圆角输入盒（醒目），内部正文不带头/尾指示线
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
            Spacer(Modifier.height(16.dp))

            // 分类
            RowScopeCatPicker(current = cat, onPick = { cat = it })
            Spacer(Modifier.height(14.dp))

            // 标签输入
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

            // 已添加标签
            if (tags.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        TagChip(tag = tag, onRemove = { tags = tags.filterNot { it == tag } }, accent = Palette.accent)
                    }
                }
            }

            // 底部操作行：左侧「删除此条」（仅编辑态，danger 链接），右侧「取消」（对齐 v2 sheet-foot）
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth()) {
                if (isEdit) {
                    Box(
                        Modifier
                            .clickable { delete() }
                            .padding(top = 6.dp, bottom = 2.dp, start = 2.dp, end = 12.dp)
                    ) {
                        Text("删除此条", color = Palette.danger, fontSize = 14.sp)
                    }
                    Spacer(Modifier.weight(1f))
                }
                Box(
                    Modifier
                        .clickable { close() }
                        .padding(top = 6.dp, bottom = 2.dp, start = 2.dp, end = 12.dp)
                ) {
                    Text("取消", color = Palette.muted, fontSize = 14.sp)
                }
            }
        }
    }
}

/** 用给定颜色填充并 clip 成圆角。 */
private fun Modifier.clipShape(shape: RoundedCornerShape, color: Color): Modifier = this.then(
    Modifier.background(color, shape)
)

private fun Modifier.clipPill(color: Color): Modifier = this.then(
    Modifier.background(color, RoundedCornerShape(50))
)
