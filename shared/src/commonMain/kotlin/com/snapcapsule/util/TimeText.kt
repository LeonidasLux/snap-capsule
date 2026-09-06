package com.snapcapsule.util

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/** 相对时间文案与「今天/近一周」的本地日界计算。 */
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

    /** 该时刻是否落在“今天”（本地自然日）。 */
    fun isToday(epochMs: Long, now: Long): Boolean =
        epochMs >= startOfDay(now)

    /** 该时刻是否在“近一周”内（今天起的 7 个自然日，即过去 6 天 + 今天）。 */
    fun isThisWeek(epochMs: Long, now: Long): Boolean =
        epochMs >= startOfDay(now - 6L * 86_400_000L) // 6 天，勿写成 6L（毫秒）

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

    /** 构建/调试戳：本地时区的「MM-dd HH:mm:ss」（两位补零）。 */
    fun stamp(epochMs: Long): String {
        val dt = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(zone)
        fun p(v: Int) = v.toString().padStart(2, '0')
        return "${p(dt.monthNumber)}-${p(dt.dayOfMonth)} ${p(dt.hour)}:${p(dt.minute)}:${p(dt.second)}"
    }
}
