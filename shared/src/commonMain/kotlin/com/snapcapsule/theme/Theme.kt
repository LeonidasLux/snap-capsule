package com.snapcapsule.theme

import com.tencent.kuikly.compose.foundation.shape.RoundedCornerShape
import com.tencent.kuikly.compose.ui.graphics.Color
import com.tencent.kuikly.compose.ui.unit.Dp
import com.tencent.kuikly.compose.ui.unit.dp

/** 设计 tokens —— 与 HTML 原型配色一一对应。 */
object Palette {
    val bg: Color = Color(0xFFF9F7F4L)        // 页面米白
    val surface: Color = Color(0xFFFFFFFFL)   // 卡片白
    val fg: Color = Color(0xFF2A2620L)        // 暖黑正文
    val muted: Color = Color(0xFF6E6A62L)     // 辅助灰
    val border: Color = Color(0xFFECE7DEL)    // 浅边线
    val accent: Color = Color(0xFF95611CL)    // 琥珀主色
    val work: Color = Color(0xFF5E6E8AL)      // 工作·蓝灰
    val life: Color = Color(0xFF55704AL)      // 生活·鼠尾草绿
    val danger: Color = Color(0xFFB44A36L)    // 删除·低饱和红
    val onAccent: Color = Color(0xFFFFFFFFL)  // 强调色上文字

    fun catColor(cat: com.snapcapsule.model.Cat): Color = when (cat) {
        com.snapcapsule.model.Cat.WORK -> work
        com.snapcapsule.model.Cat.LIFE -> life
    }

    fun catSoft(cat: com.snapcapsule.model.Cat): Color = when (cat) {
        com.snapcapsule.model.Cat.WORK -> Color(0x245E6E8AL) // alpha ~14%
        com.snapcapsule.model.Cat.LIFE -> Color(0x2455744AL)
    }

    val accentSoft: Color = Color(0x2495611CL)
    val dangerSoft: Color = Color(0x24B44A36L)
    val lifeSoft: Color = Color(0x2455744AL)
    val fgSoft: Color = Color(0x0D2A2620L)
}

object Radius {
    val card = 14.dp
    val sheetTop = 20.dp
    val chip = 12.dp
    val btn = 12.dp
}

/** 24px 高 chip 标签的圆角参数（高 24 · 圆角 12 由外层 clip 控制）。 */
object TypeScale {
    val title = 18.dp
    val body = 15.dp
    val tag = 12.dp
    val time = 12.dp
}
