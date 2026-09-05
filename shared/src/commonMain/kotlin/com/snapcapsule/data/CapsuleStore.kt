package com.snapcapsule.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.snapcapsule.model.Capsule
import com.snapcapsule.model.Cat
import com.snapcapsule.model.Filter
import com.snapcapsule.model.Status
import com.snapcapsule.util.TimeText
import kotlinx.datetime.Clock

/**
 * 全局胶囊 Store：compose snapshot 状态 + 单 JSON 持久化。
 * UI 读取 [visible]/[all] 即订阅重组；所有变更函数立即落盘。
 */
object CapsuleStore {

    private var listState by mutableStateOf<List<Capsule>>(emptyList())
    var filter by mutableStateOf(Filter.ALL)
        private set

    private var loaded = false

    val all: List<Capsule> get() = listState
    val totalCount: Int get() = listState.size
    val activeCount: Int get() = listState.count { it.status == Status.ACTIVE }
    val archivedCount: Int get() = listState.count { it.status == Status.ARCHIVED }
    val trashedCount: Int get() = listState.count { it.status == Status.TRASHED }

    fun now(): Long = Clock.System.now().toEpochMilliseconds()

    /** 启动时调用一次：读盘 → 校验 → 载入（非法/空则保持空态）。 */
    fun ensureLoaded() {
        if (loaded) return
        loaded = true
        val raw = CapsuleFileStorage.read()
        if (raw == null) return
        when (val r = JsonCodec.decode(raw)) {
            is DecodeResult.Ok -> listState = r.capsules.sortedByDescending { it.createdAt }
            is DecodeResult.Err -> Unit // 损坏则从空开始，不覆盖原文件
        }
    }

    /** 当前 Tab 应展示的胶囊（排序：createdAt 降序）。 */
    fun visible(): List<Capsule> {
        val n = now()
        return listState.filter { c ->
            when (filter) {
                Filter.ALL -> c.status == Status.ACTIVE
                Filter.TODAY -> c.status == Status.ACTIVE && TimeText.isToday(c.createdAt, n)
                Filter.WEEK -> c.status == Status.ACTIVE && TimeText.isThisWeek(c.createdAt, n)
                Filter.ARCHIVED -> c.status == Status.ARCHIVED
            }
        }.sortedByDescending { it.createdAt }
    }

    fun applyFilter(f: Filter) {
        filter = f
    }

    fun find(id: Long): Capsule? = listState.firstOrNull { it.id == id }

    // ── 变更 ─────────────────────────────────────────────

    fun add(text: String, cat: Cat, tags: List<String>) {
        val c = Capsule(id = nextId(), text = text.trim(), cat = cat, tags = tags, createdAt = now())
        listState = listOf(c) + listState
        persist()
    }

    fun update(id: Long, text: String, cat: Cat, tags: List<String>) {
        listState = listState.map {
            if (it.id == id) it.copy(text = text.trim(), cat = cat, tags = tags) else it
        }
        persist()
    }

    fun archive(id: Long) {
        listState = listState.map { if (it.id == id) it.copy(status = Status.ARCHIVED) else it }
        persist()
    }

    fun updateCat(id: Long, cat: Cat) {
        listState = listState.map { if (it.id == id) it.copy(cat = cat) else it }
        persist()
    }

    /** 恢复：归档箱/回收站的条目统一回到主页（ACTIVE）。 */
    fun restore(id: Long) {
        listState = listState.map { if (it.id == id) it.copy(status = Status.ACTIVE) else it }
        persist()
    }

    /** 移入回收站（软删除）：主页与归档箱点「删除」即置 TRASHED，可从回收站恢复。 */
    fun trash(id: Long) {
        listState = listState.map { if (it.id == id) it.copy(status = Status.TRASHED) else it }
        persist()
    }

    /** 回收站当前内容（创建时间降序）。 */
    fun trashed(): List<Capsule> =
        listState.filter { it.status == Status.TRASHED }.sortedByDescending { it.createdAt }

    /** 彻底删除一条：仅从回收站调用，不可恢复。 */
    fun purge(id: Long) {
        listState = listState.filterNot { it.id == id }
        persist()
    }

    /** 清空回收站：全部彻底删除。 */
    fun emptyTrash() {
        listState = listState.filterNot { it.status == Status.TRASHED }
        persist()
    }

    fun clearAll() {
        listState = emptyList()
        persist()
    }

    /** 合并导入（同 id 以导入版本覆盖）。 */
    fun merge(imported: List<Capsule>) {
        val importedIds = imported.mapTo(mutableSetOf()) { it.id }
        val kept = listState.filterNot { it.id in importedIds }
        listState = (kept + imported).sortedByDescending { it.createdAt }
        persist()
    }

    /** 覆盖式导入。 */
    fun replaceAll(imported: List<Capsule>) {
        listState = imported.sortedByDescending { it.createdAt }
        persist()
    }

    /** 空列表时载入示例数据（便于演示/截图）。返回是否已填入。 */
    fun loadSampleIfEmpty(): Boolean {
        if (listState.isNotEmpty()) return false
        listState = JsonCodec.loadSample(now())
        persist()
        return true
    }

    /** 导出全文（含归档、回收站）即当前全部数据。 */
    fun exportJson(): String = JsonCodec.encode(listState)

    fun importText(text: String): DecodeResult = JsonCodec.decode(text)

    // ── 内部 ─────────────────────────────────────────────

    private fun persist() {
        CapsuleFileStorage.write(JsonCodec.encode(listState))
    }

    private fun nextId(): Long = (listState.maxOfOrNull { it.id } ?: 0L) + 1L
}
