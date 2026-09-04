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
import com.tencent.kuikly.compose.foundation.layout.Row
import com.tencent.kuikly.compose.foundation.layout.Spacer
import com.tencent.kuikly.compose.foundation.layout.fillMaxWidth
import com.tencent.kuikly.compose.foundation.layout.height
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.layout.width
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Card
import com.tencent.kuikly.compose.material3.CardDefaults
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.text.font.FontWeight
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.snapcapsule.data.CapsuleStore
import com.snapcapsule.theme.Palette
import com.snapcapsule.theme.Radius

/** 删除单条确认。 */
@Composable
fun DeleteConfirmDialog() {
    val id = UiState.confirmDeleteId
    if (id == null) return
    ConfirmCard(
        title = "删除这条胶囊？",
        sub = "删除后将无法恢复",
        confirmText = "删除",
        confirmColor = Palette.danger,
        onDismiss = { UiState.confirmDeleteId = null },
        onConfirm = {
            CapsuleStore.delete(id)
            UiState.confirmDeleteId = null
            UiState.toast("已删除")
        },
    )
}

/** 清空全部确认。 */
@Composable
fun ClearAllConfirmDialog() {
    if (!UiState.confirmClear) return
    ConfirmCard(
        title = "清空全部胶囊？",
        sub = "共 ${CapsuleStore.totalCount} 条将全部删除，且无法恢复",
        confirmText = "清空",
        confirmColor = Palette.danger,
        onDismiss = { UiState.confirmClear = false },
        onConfirm = {
            CapsuleStore.clearAll()
            UiState.confirmClear = false
            UiState.toast("已清空")
        },
    )
}

/** 导入确认：合并 / 覆盖 / 取消。 */
@Composable
fun ImportOfferDialog() {
    val offer = ImportUi.offer ?: return
    val n = offer.capsules.size

    Dialog(onDismissRequest = { ImportUi.offer = null }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("导入数据", color = Palette.fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("文件含 ${n} 条胶囊，如何导入？", color = Palette.muted, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth()) {
                    DialogBtn("合并", Modifier.weight(1f), Palette.fg) {
                        CapsuleStore.merge(offer.capsules)
                        ImportUi.offer = null
                        UiState.toast("已合并 ${n} 条")
                    }
                    Spacer(Modifier.width(10.dp))
                    DialogBtn("覆盖", Modifier.weight(1f), Palette.danger) {
                        CapsuleStore.replaceAll(offer.capsules)
                        ImportUi.offer = null
                        UiState.toast("已覆盖为 ${n} 条")
                    }
                }
                Spacer(Modifier.height(6.dp))
                Box(Modifier.padding(8.dp).clickable { ImportUi.offer = null }) {
                    Text("取消", color = Palette.muted, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun ConfirmCard(
    title: String,
    sub: String,
    confirmText: String,
    confirmColor: Color,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(top = 22.dp, bottom = 14.dp, start = 20.dp, end = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(title, color = Palette.fg, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text(sub, color = Palette.muted, fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                Row(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier.weight(1f).height(44.dp).background(Palette.fgSoft, RoundedCornerShape(12.dp))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("取消", color = Palette.fg, fontSize = 15.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier.weight(1f).height(44.dp).background(confirmColor, RoundedCornerShape(12.dp))
                            .clickable { onConfirm() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(confirmText, color = Palette.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogBtn(label: String, modifier: Modifier = Modifier, color: Color, onClick: () -> Unit) {
    Box(
        modifier.background(color, RoundedCornerShape(12.dp)).clickable { onClick() }.padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Palette.onAccent, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
    }
}
