package com.snapcapsule.data

import com.snapcapsule.model.Capsule
import com.snapcapsule.model.Cat
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** 磁盘/导出统一 JSON schema v1。 */
@Serializable
data class CapsuleFile(
    val version: Int = 1,
    val capsules: List<Capsule> = emptyList(),
)

/** 导入解析结果。 */
sealed interface DecodeResult {
    data class Ok(val capsules: List<Capsule>) : DecodeResult
    data class Err(val reason: String) : DecodeResult
}

object JsonCodec {

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    fun encode(capsules: List<Capsule>): String =
        json.encodeToString(CapsuleFile(capsules = capsules))

    /**
     * 全量校验：版本 / 必填字段 / 值域均合法才返回 Ok，否则 Err。
     * 调用方保证「全量合法才落盘」，绝不半写坏数据。
     */
    fun decode(text: String): DecodeResult {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return DecodeResult.Err("文件为空")

        val file = try {
            json.decodeFromString<CapsuleFile>(trimmed)
        } catch (t: Throwable) {
            return DecodeResult.Err("不是有效的 JSON 文件（${t.message?.take(60) ?: "解析失败"}）")
        }

        if (file.version != 1) {
            return DecodeResult.Err("不支持的数据版本 ${file.version}，仅支持 v1")
        }
        if (file.capsules.isEmpty()) return DecodeResult.Ok(emptyList())

        // 校验每条记录；任何一条非法即整体拒绝（回滚语义）
        file.capsules.forEachIndexed { i, c ->
            val reason = when {
                c.id <= 0L -> "第 ${i + 1} 条 id 非法"
                c.text.isBlank() -> "第 ${i + 1} 条内容为空"
                c.createdAt <= 0L -> "第 ${i + 1} 条时间非法"
                c.tags.any { it.isBlank() } -> "第 ${i + 1} 条含空标签"
                else -> null
            }
            if (reason != null) return DecodeResult.Err(reason)
        }
        // 合并重复 id（保留靠后者，避免导入后键冲突）
        val merged = file.capsules.associateBy { it.id }.values.toList()
        return DecodeResult.Ok(merged)
    }

    /**
     * 示例数据（原型 SEED 的五条，语义一致；仅用于首次体验/演示载入）。
     * now = 当前 epochMs，用于生成相对时间一致的 createdAt。
     */
    fun loadSample(now: Long): List<Capsule> {
        val hour = 3600_000L
        val minute = 60_000L
        val day = 24 * hour
        fun mk(id: Long, text: String, cat: Cat, tags: List<String>, ago: Long) =
            Capsule(id = id, text = text, cat = cat, tags = tags, createdAt = now - ago)

        return listOf(
            mk(101, "整理下周产品评审会的待办清单，把用户反馈里的高频问题一并带上", Cat.WORK, listOf("工作", "会议"), 18 * hour + 40 * minute),
            mk(102, "周末想去城西新开的书店，听说有很好喝的燕麦拿铁", Cat.LIFE, listOf("生活"), 3 * day + 2 * hour),
            mk(103, "和设计团队确认新版配色，主色调倾向低饱和琥珀，避免大红大蓝", Cat.WORK, listOf("设计", "配色"), 6 * day),
            mk(104, "给妈妈打个电话，问问她最近的血压情况", Cat.LIFE, listOf("家庭"), 10 * day),
            mk(105, "研究竞品的归档交互，我们的右滑归档可以借鉴", Cat.WORK, listOf("调研"), 14 * day),
        )
    }
}
