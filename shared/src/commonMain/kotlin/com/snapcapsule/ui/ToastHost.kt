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
import com.tencent.kuikly.compose.ui.unit.dp
import com.tencent.kuikly.compose.ui.unit.sp
import com.snapcapsule.data.CapsuleStore
import com.snapcapsule.theme.Palette
import kotlinx.coroutines.delay

/** 底部 Toast。 */
@Composable
fun ToastHost() {
    LaunchedEffect(UiState.toastSeq) {
        if (UiState.toastSeq > 0) {
            delay(1900)
            UiState.toastText = null
        }
    }
    val text = UiState.toastText ?: return
    Box(
        Modifier
            .fillMaxSize()
            .padding(bottom = 110.dp),
        contentAlignment = Alignment.BottomCenter,
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
