package com.snapcapsule.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/** 胶囊分类：与原型一致，仅「工作 / 生活」两枚。 */
enum class Cat {
    @SerialName("work")
    WORK,

    @SerialName("life")
    LIFE,
}

/** 主页筛选 Tab：与 v2 原型一致，按「完成」与否分两枚。 */
enum class Filter(val label: String) {
    OPEN("未完成"),
    DONE("已完成"),
}

/**
 * 胶囊。删除归档三态，改为两个独立的布尔维度（对齐 v2 原型）：
 *  - [done]    是否已完成：true 进「已完成」Tab；false 在「未完成」。可随时勾回。
 *  - [trashed] 是否在回收站（软删除，可恢复）：恢复时保留原 [done]，回到原 Tab。
 */
@Serializable
data class Capsule(
    val id: Long,
    val text: String,
    val cat: Cat,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val done: Boolean = false,
    val trashed: Boolean = false,
)
