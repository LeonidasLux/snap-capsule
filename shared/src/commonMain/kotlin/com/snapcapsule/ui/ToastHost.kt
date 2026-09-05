package com.snapcapsule.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.tencent.kuikly.compose.foundation.background
import com.tencent.kuikly.compose.foundation.layout.Box
import com.tencent.kuikly.compose.foundation.layout.fillMaxSize
import com.tencent.kuikly.compose.foundation.layout.padding
import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.material3.Text
import com.tencent.kuikly.compose.ui.Alignment
import com.tencent.kuikly.compose.ui.Modifier
import com.tencent.kuikly.compose.ui.draw.clip
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.tencent.kuikly.compose.ui.window.Dialog
import com.tencent.kuikly.compose.ui.window.DialogProperties
import com.snapcapsule.theme.Palette
import kotlinx.coroutines.delay

/**
 * 页面顶部 Toast。
 *
 * ModalBottomSheet / 确认框都开在独立 Dialog 窗口里，普通布局层的内容会被它们的遮罩盖住，
 * 所以这里自己也用 Dialog：后开的窗口叠在最上，弹层打开时提示同样可见。
 * 透明 scrim（不压暗背景）、点空白不关闭，仅 1.9s 后自动消失。
 */
@Composable
fun ToastHost() {
    val text = UiState.toastText ?: return
    LaunchedEffect(UiState.toastSeq) {
        delay(1900)
        UiState.toastText = null
    }
    Dialog(
        onDismissRequest = { UiState.toastText = null },
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            scrimColor = Color.Transparent,
        ),
    ) {
        Box(
            Modifier.fillMaxSize().padding(top = 48.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Palette.fg)
                    .padding(horizontal = 18.dp, vertical = 11.dp)
            ) {
                Text(text, color = Palette.surface, fontSize = 13.sp)
            }
        }
    }
}
