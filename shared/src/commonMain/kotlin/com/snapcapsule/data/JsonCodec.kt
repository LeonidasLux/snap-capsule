package com.snapcapsule.data

import com.snapcapsule.model.Capsule
import com.snapcapsule.model.Cat
import com.snapcapsule.model.Status
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
     * 示例数据：50 条，时间从几分钟前到约 3 年前（横跨三年），
     * 其中 5 条已归档便于演示归档箱。now = 当前 epochMs，用于生成相对时间一致的 createdAt。
     */
    fun loadSample(now: Long): List<Capsule> {
        val hour = 3600_000L
        val minute = 60_000L
        val day = 24 * hour
        fun mk(
            id: Long,
            text: String,
            cat: Cat,
            tags: List<String>,
            ago: Long,
            status: Status = Status.ACTIVE,
        ) = Capsule(id = id, text = text, cat = cat, tags = tags, createdAt = now - ago, status = status)

        return listOf(
            // —— 今天 · 近一周（新 → 旧） ——
            mk(201, "把测试反馈的首页空态文案对比度问题转给前端，附上复现路径和录屏链接", Cat.WORK, listOf("工作", "沟通"), 1 * minute),
            mk(202, "记得取今晚话剧的票，19:30 前到剧场，地铁 E 口出", Cat.LIFE, listOf("生活"), 8 * minute),
            mk(203, "灰度方案补充一个回归对照组，避免把大盘波动误判成实验收益", Cat.WORK, listOf("工作", "数据分析"), 2 * hour),
            mk(204, "给阳台的多肉换盆，顺便补一次缓释肥", Cat.LIFE, listOf("生活", "家居"), 5 * hour + 30 * minute),
            mk(205, "把《注意力是新的货币》里的要点摘进下周分享", Cat.WORK, listOf("阅读"), 11 * hour),
            mk(206, "妈妈生日前挑个按摩仪，预算 800 以内，避开噪音大的那几款", Cat.LIFE, listOf("家庭"), 1 * day + 3 * hour),
            mk(207, "梳理 Q3 复盘数据，标注三个超预期的改动点", Cat.WORK, listOf("工作", "复盘"), 1 * day + 20 * hour),
            mk(208, "约老周周六早上去爬山，天气好的话顺带拍日出", Cat.LIFE, listOf("朋友"), 3 * day),
            mk(209, "竞品新版本加了快捷手势，原型里加一版左滑置顶做 A/B 素材", Cat.WORK, listOf("产品", "调研"), 4 * day + 6 * hour),
            mk(210, "把旧手机照片备份到网盘，腾出空间再给孩子录几条语音", Cat.LIFE, listOf("数码"), 5 * day + 9 * hour),
            // —— 近一个月 ——
            mk(211, "面试题库补一道系统设计：如何支撑十万人同时抢课", Cat.WORK, listOf("面试"), 8 * day),
            mk(212, "体检报告取回来，重点看甲状腺和血脂两项，找医生复核", Cat.LIFE, listOf("健康"), 12 * day),
            mk(213, "给 API 加上请求日志采样，定位慢接口先看 p99 分布", Cat.WORK, listOf("代码"), 16 * day),
            mk(214, "家里路由器重启后仍掉线，联系运营商上门检测光猫", Cat.LIFE, listOf("家居"), 21 * day),
            mk(215, "版本发布前把数据库备份脚本在预发环境完整跑通一次", Cat.WORK, listOf("运维"), 26 * day),
            mk(216, "灵感：把相机改成「先按快门再构图」的模式，拍街景更随性", Cat.LIFE, listOf("灵感"), 33 * day),
            // —— 近一个季度 ——
            mk(217, "把用户访谈里「导出后找不到文件」的反馈整理成独立工单", Cat.WORK, listOf("产品"), 41 * day),
            mk(218, "给爸妈写一页图文，教他们识别 AI 换脸诈骗电话", Cat.LIFE, listOf("家庭"), 50 * day),
            mk(219, "把设计稿的间距统一收敛到 4/8/12 的栅格体系", Cat.WORK, listOf("设计"), 61 * day),
            mk(220, "附近新开的川菜馆约同事去试，先记着人均和营业时间", Cat.LIFE, listOf("生活"), 74 * day),
            mk(221, "优化冷启动：把首屏用不到的图表库拆成按需加载", Cat.WORK, listOf("代码", "性能"), 88 * day),
            mk(222, "基金定投继续，调低消费板块仓位到三成", Cat.LIFE, listOf("理财"), 104 * day),
            // —— 近半年 ——
            mk(223, "周报模板加一栏「本周阻塞」，让风险更早浮出水面", Cat.WORK, listOf("复盘"), 122 * day),
            mk(224, "把客厅灯换成可调色温的，晚上看书更舒服", Cat.LIFE, listOf("家居"), 142 * day),
            mk(225, "把竞品的签到体系画成流程图，找我们的留存切入点", Cat.WORK, listOf("调研"), 164 * day),
            mk(226, "夏天前把跑步里程拉回每周三次，先从三公里开始", Cat.LIFE, listOf("健康"), 188 * day),
            mk(227, "重构权限模块前先补一轮用例，避免回归把老逻辑打坏", Cat.WORK, listOf("代码"), 214 * day),
            mk(228, "报了个周末陶艺课，把一直想做的杯子做出来", Cat.LIFE, listOf("学习"), 242 * day),
            // —— 近一年 ——
            mk(229, "把客服高频问题沉淀成帮助中心文章，减少重复咨询", Cat.WORK, listOf("文档"), 272 * day),
            mk(230, "寒假带爸妈去南方海边，避开春节人潮", Cat.LIFE, listOf("家庭", "旅行"), 304 * day),
            mk(231, "手势冲突评审：确认长按与左滑在不同场景的边界", Cat.WORK, listOf("会议"), 338 * day),
            mk(232, "把闲置的 Kindle 挂出去，换成能看批注的墨水屏", Cat.LIFE, listOf("数码"), 374 * day),
            mk(233, "研究信息流对长内容的适配，输出一页结论", Cat.WORK, listOf("调研"), 412 * day),
            mk(234, "戒了睡前刷短视频两周，睡眠明显变好，继续坚持", Cat.LIFE, listOf("健康"), 452 * day),
            // —— 一年以上 ——
            mk(235, "给新人梳理一份 onboarding 清单，覆盖从账号到发布", Cat.WORK, listOf("文档"), 494 * day),
            mk(236, "秋游那个水库边可以钓鱼，下次带上装备", Cat.LIFE, listOf("旅行"), 538 * day),
            mk(237, "把埋点规范补充到 wiki，事件命名统一「模块.动作」", Cat.WORK, listOf("文档"), 584 * day),
            mk(238, "记下那本讲慢决策的书里的两个反例", Cat.LIFE, listOf("阅读"), 632 * day, Status.ARCHIVED),
            mk(239, "旧版小程序兼容：给即将废弃的接口加灰度提醒", Cat.WORK, listOf("运维"), 682 * day),
            mk(240, "办健身卡前先蹭两周体验课，确认能坚持再决定", Cat.LIFE, listOf("健康"), 734 * day),
            // —— 两年前 ——
            mk(241, "把测试环境造数脚本参数化，减少手工拼 JSON", Cat.WORK, listOf("代码"), 788 * day),
            mk(242, "给车窗换膜，比价三家再定，问清质保年限", Cat.LIFE, listOf("数码"), 844 * day),
            mk(243, "把去年的 OKR 复盘结论整理成一页纸，给今年定目标做参照", Cat.WORK, listOf("复盘"), 902 * day),
            mk(244, "第一次跑半马完赛，记下后半程补给的节奏心得", Cat.LIFE, listOf("健康"), 962 * day, Status.ARCHIVED),
            mk(245, "图片 CDN 切到新服务商前，先跑一周双写比对", Cat.WORK, listOf("运维"), 1024 * day),
            // —— 三年前（最早的一批） ——
            mk(246, "把旅行照片按城市归档，每城挑十张做成相册", Cat.LIFE, listOf("旅行"), 1088 * day),
            mk(247, "整理技术分享初稿，补充图表后投公司内刊", Cat.WORK, listOf("文档"), 1102 * day),
            mk(248, "搬进新家第一周：记下厨房下水慢，要请师傅通一次", Cat.LIFE, listOf("家居"), 1110 * day, Status.ARCHIVED),
            mk(249, "重构支付回调的幂等逻辑，先写清边界再动代码", Cat.WORK, listOf("代码"), 1118 * day, Status.ARCHIVED),
            mk(250, "第一次露营：帐篷别搭在风口，防潮垫要带够", Cat.LIFE, listOf("旅行"), 1130 * day, Status.ARCHIVED),
        )
    }
}
