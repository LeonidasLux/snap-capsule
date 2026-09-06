package com.snapcapsule.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** 相对时间文案（卡片脚注）。 */
object TimeText {

    private val zone: TimeZone = TimeZone.currentSystemDefault()

    /**
     * 相对文案：刚刚 / n 分钟前 / n 小时前 / 昨天 / n 天前 / 超过 30 天显示日期。
     */
    fun relative(epochMs: Long, now: Long): String {
        val diff = now - epochMs
        if (diff < 60_000L) return "刚刚"
        val minute = diff / 60_000L
        if (minute < 60L) return "${minute}分钟前"
        val hour = minute / 60L
        if (hour < 24L) return "${hour}小时前"
        val todayStart = startOfDay(now)
        val yesterdayStart = startOfDay(now - 1L)
        return when {
            epochMs >= todayStart -> "${hour / 24L}天前"
            epochMs >= yesterdayStart -> "昨天"
            else -> {
                val days = (todayStart - epochMs) / 86_400_000L
                if (days < 30L) "${days}天前"
                else dateText(epochMs)
            }
        }
    }

    private fun startOfDay(epochMs: Long): Long {
        val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
        val dayStart = LocalDateTime(dt.date, LocalTime(hour = 0, minute = 0))
        return dayStart.toInstant(zone).toEpochMilliseconds()
    }

    private fun dateText(epochMs: Long): String {
        val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
        val thisYear = Clock.System.now().toLocalDateTime(zone).year
        val y = if (dt.year == thisYear) "" else "${dt.year}年"
        return "${y}${dt.monthNumber}月${dt.dayOfMonth}日"
    }

    /**
     * 构建/调试戳：北京时间的「MM-dd HH:mm:ss」（两位补零）。
     * 北京 = UTC+8 且无夏令时：把 epoch 平移 +8h 后在 UTC 下读墙钟即可。
     * 不沿用设备时区（模拟器默认 GMT，会把构建时刻显示成早 8 小时 / 跨前一天），也不引入 IANA 时区依赖。
     */
    fun stamp(epochMs: Long): String {
        val dt = Instant.fromEpochMilliseconds(epochMs + 8L * 3_600_000L).toLocalDateTime(TimeZone.UTC)
        fun p(v: Int) = v.toString().padStart(2, '0')
        return "${p(dt.monthNumber)}-${p(dt.dayOfMonth)} ${p(dt.hour)}:${p(dt.minute)}:${p(dt.second)}"
    }

    /**
     * 抽屉「更多」里的完整时间：设备本地时区「M月D日 HH:mm」（对齐 v2-1 原型的 stamp）。
     * 胶囊是用户数据，展示按其设备时区，与 [stamp]（北京固定给构建 header）不同。
     */
    fun meta(epochMs: Long): String {
        val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
        fun p(v: Int) = v.toString().padStart(2, '0')
        return "${dt.monthNumber}月${dt.dayOfMonth}日 ${p(dt.hour)}:${p(dt.minute)}"
    }
}
