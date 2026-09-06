package com.snapcapsule.data

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.snapcapsule.model.Capsule
import com.snapcapsule.model.Cat
import com.snapcapsule.model.Filter
import kotlinx.datetime.Clock

/**
 * 全局胶囊 Store：compose snapshot 状态 + 单 JSON 持久化。
 * UI 读取 [visible]/[all] 即订阅重组；所有变更函数立即落盘。
 * 状态语义（对齐 v2）：未完成 / 已完成 两枚 Tab；删除进回收站可恢复。
 */
object CapsuleStore {

    private var listState by mutableStateOf<List<Capsule>>(emptyList())
    var filter by mutableStateOf(Filter.OPEN)
        private set

    private var loaded = false

    val all: List<Capsule> get() = listState
    val totalCount: Int get() = listState.size
    val trashedCount: Int get() = listState.count { it.trashed }

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
    fun visible(): List<Capsule> = listState.filter { c ->
        !c.trashed && when (filter) {
            Filter.OPEN -> !c.done
            Filter.DONE -> c.done
        }
    }.sortedByDescending { it.createdAt }

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

    /** 勾选/取消完成：未完成 → 已完成（进「已完成」Tab），反之亦然。 */
    fun setDone(id: Long, done: Boolean) {
        listState = listState.map { if (it.id == id) it.copy(done = done) else it }
        persist()
    }

    /** 移入回收站（软删除，可恢复）；恢复时保留原 done 状态。 */
    fun trash(id: Long) {
        listState = listState.map { if (it.id == id) it.copy(trashed = true) else it }
        persist()
    }

    /** 恢复：回收站条目回到主页（保留未完成/已完成，进对应 Tab）。 */
    fun restore(id: Long) {
        listState = listState.map { if (it.id == id) it.copy(trashed = false) else it }
        persist()
    }

    /** 回收站当前内容（创建时间降序）。 */
    fun trashed(): List<Capsule> =
        listState.filter { it.trashed }.sortedByDescending { it.createdAt }

    /** 彻底删除一条：仅从回收站调用，不可恢复。 */
    fun purge(id: Long) {
        listState = listState.filterNot { it.id == id }
        persist()
    }

    /** 清空回收站：全部彻底删除。 */
    fun emptyTrash() {
        listState = listState.filterNot { it.trashed }
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

    /** 导出全文（含回收站）即当前全部数据。 */
    fun exportJson(): String = JsonCodec.encode(listState)

    fun importText(text: String): DecodeResult = JsonCodec.decode(text)

    // ── 内部 ─────────────────────────────────────────────

    private fun persist() {
        CapsuleFileStorage.write(JsonCodec.encode(listState))
    }

    private fun nextId(): Long = (listState.maxOfOrNull { it.id } ?: 0L) + 1L
}
