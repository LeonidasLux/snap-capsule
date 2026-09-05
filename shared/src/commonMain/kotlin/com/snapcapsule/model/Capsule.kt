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

/** 状态：active 在主页展示；archived 进归档箱；trashed 在回收站（软删除，可恢复）。 */
enum class Status {
    @SerialName("active")
    ACTIVE,

    @SerialName("archived")
    ARCHIVED,

    @SerialName("trashed")
    TRASHED,
}

/** 顶部筛选 Tab。 */
enum class Filter(val label: String) {
    TODAY("今天"),
    WEEK("近一周"),
    ALL("全部"),
    ARCHIVED("归档"),
}

@Serializable
data class Capsule(
    val id: Long,
    val text: String,
    val cat: Cat,
    val tags: List<String> = emptyList(),
    val createdAt: Long,
    val status: Status = Status.ACTIVE,
)
