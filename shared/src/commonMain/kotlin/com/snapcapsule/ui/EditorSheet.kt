package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.clickable
import com.tencent.kuikly.compose.foundation.layout.Arrangement
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.Column
import com.tencent.kuikly.compose.foundation.layout.ExperimentalLayoutApi
import com.tencent.kuikly.compose.foundation.layout.FlowRow
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
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

private val catKeyLabel = mapOf(Cat.LIFE to "生活", Cat.WORK to "工作")

/** 新建 / 编辑半屏弹窗。 */
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

    ModalBottomSheet(
        visible = true,
        onDismissRequest = { UiState.editorVisible = false; UiState.editingId = null },
        modifier = Modifier,
        containerColor = Palette.surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp)) {
            // 顶栏
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
                        .clipPill(Palette.accent)
                        .clickable { save() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text("保存", color = Palette.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(6.dp))

            // 正文
            TextField(
                value = textValue,
                onValueChange = { textValue = it },
                placeholder = { Text("记个闪念…", color = Palette.muted) },
                maxLines = 6,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Palette.border,
                    cursorColor = Palette.accent,
                ),
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )

            // 分类
            RowScopeCatPicker(current = cat, onPick = { cat = it })
            Spacer(Modifier.height(16.dp))

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
            Spacer(Modifier.height(8.dp))

            // 已添加标签
            if (tags.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    tags.forEach { tag ->
                        TagChip(tag = tag, onRemove = { tags = tags.filterNot { it == tag } }, accent = Palette.accent)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))

            // 取消
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Box(
                    Modifier
                        .clickable {
                            UiState.editorVisible = false
                            UiState.editingId = null
                        }
                        .padding(10.dp)
                ) {
                    Text("取消", color = Palette.muted, fontSize = 14.sp)
                }
            }
        }
    }
}

private fun Modifier.clipPill(color: Color): Modifier = this.then(
    Modifier.background(color, RoundedCornerShape(50))
)
