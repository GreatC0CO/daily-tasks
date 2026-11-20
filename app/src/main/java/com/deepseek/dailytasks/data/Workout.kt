package com.deepseek.dailytasks.data

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 动作可选的标记颜色（索引 1..6，0 = 不标色）。 */
val EXERCISE_COLORS: List<Long> = listOf(
    0xFF2E6BE6L, // 蓝
    0xFF2FA84FL, // 绿
    0xFFF0A020L, // 橙
    0xFFD64545L, // 红
    0xFF8E5BEFL, // 紫
    0xFF12A0A8L, // 青
)

/**
 * 动作的唯一标识：名称 + 次数 + 重量。
 *
 * 同名但次数/重量不同的动作是两个不同的动作，各自保留进度、各自显示，
 * 只有这三项都相同的才算同一个（合并时也只合并这种）。
 */
fun exerciseKey(name: String, reps: String, weight: Double): String =
    "${name.trim().lowercase()}|${reps.trim().lowercase()}|$weight"

/** 一个动作：名称、目标组数、每组次数（"12"、"45 秒"、"30 分钟" 都可以）、目标重量（公斤，0 = 自重/不记重量）。 */
data class Exercise(
    val name: String,
    val sets: Int = 3,
    val reps: String = "12",
    val weight: Double = 0.0,
    /** 标记颜色：0 = 不标，1..6 对应 [EXERCISE_COLORS]。 */
    val color: Int = 0,
) {
    val key: String
        get() = exerciseKey(name, reps, weight)

    /** 有没有记重量（0 表示自重或没填）。 */
    val hasWeight: Boolean
        get() = weight > 0.0

    /** "20kg" / "22.5kg" / "自重" */
    val weightText: String
        get() = if (!hasWeight) "自重" else formatWeight(weight)

    val targetText: String
        get() = "$sets 组 × $reps"

    /** 带重量的一行说明："4 组 × 8 · 60kg" */
    val detailText: String
        get() = if (hasWeight) "$targetText · $weightText" else targetText
}

/** 今日训练里的一个动作（含已完成组数、今天的实际重量）。 */
data class SessionEntry(
    val name: String,
    val sets: Int,
    val reps: String,
    val doneSets: Int = 0,
    val weight: Double = 0.0,
    val color: Int = 0,
) {
    val key: String
        get() = exerciseKey(name, reps, weight)

    /** 已完成组数，限制在 0..sets */
    val done: Int
        get() = doneSets.coerceIn(0, sets)

    val isComplete: Boolean
        get() = done >= sets

    val hasWeight: Boolean
        get() = weight > 0.0

    val weightText: String
        get() = if (!hasWeight) "自重" else formatWeight(weight)

    /** 已完成部分的容量（组 × 次 × 重量）：没记重量时是 0。 */
    val volumeKg: Double
        get() = if (!hasWeight) 0.0 else done * repsCount(reps) * weight

    val volumeText: String
        get() = if (volumeKg > 0.0) String.format(Locale.US, "%,.0fkg", volumeKg) else "—"

    val progressText: String
        get() = "$done/$sets"

    val targetText: String
        get() = "$sets 组 × $reps"

    val detailText: String
        get() = if (hasWeight) "$targetText · $weightText" else targetText
}

/** 从 "8"、"8-10"、"12 次" 里取第一个整数，"45 秒" 这种也取 45（只用于估算容量）。 */
fun repsCount(reps: String): Int {
    val digits = Regex("\\d+").find(reps)?.value ?: return 0
    return digits.toIntOrNull() ?: 0
}

/** 重量统一显示成 "20kg" / "22.5kg"，去掉多余的 0。 */
fun formatWeight(weight: Double): String {
    val text = if (weight == weight.toLong().toDouble()) {
        weight.toLong().toString()
    } else {
        String.format(Locale.US, "%.1f", weight)
    }
    return "${text}kg"
}

/** 训练容量（组×次×重量），大数值加千分位。 */
fun formatVolume(volume: Double): String =
    if (volume <= 0.0) "—" else String.format(Locale.US, "%,.0fkg", volume)

/** N 选一的编号符号：① ② ③ …（超过 20 个时回落成 (n)）。 */
val VARIANT_MARKS: List<String> = listOf(
    "①", "②", "③", "④", "⑤", "⑥", "⑦", "⑧", "⑨", "⑩",
    "⑪", "⑫", "⑬", "⑭", "⑮", "⑯", "⑰", "⑱", "⑲", "⑳",
)

fun variantMark(index: Int): String = VARIANT_MARKS.getOrElse(index) { "(${index + 1})" }

/** 模板第二层（同一动作的几种练法）用的编号：罗马数字 Ⅰ Ⅱ Ⅲ … */
val ROMAN_MARKS: List<String> = listOf(
    "Ⅰ", "Ⅱ", "Ⅲ", "Ⅳ", "Ⅴ", "Ⅵ", "Ⅶ", "Ⅷ", "Ⅸ", "Ⅹ", "Ⅺ", "Ⅻ",
)

fun romanMark(index: Int): String = ROMAN_MARKS.getOrElse(index) { "(${index + 1})" }

/** 一个部位对应的训练计划。 */
data class WorkoutPlan(
    val id: String,
    val part: String,
    val title: String,
    val exercises: List<Exercise>,
    val custom: Boolean = false,
)

/** 今日训练草稿：选中的部位 + 当前进度，随时保存，切页面/杀进程都不丢。 */
data class WorkoutDraft(
    val epochDay: Long,
    val parts: List<String>,
    val entries: List<SessionEntry>,
) {
    val totalDone: Int
        get() = entries.sumOf { it.done }

    val totalPlanned: Int
        get() = entries.sumOf { it.sets }

    /** 今天已经练掉的容量（kg）。 */
    val totalVolume: Double
        get() = entries.sumOf { it.volumeKg }
}

/** 一次已完成的训练记录。 */
data class WorkoutSession(
    val id: Long,
    val epochDay: Long,
    val parts: List<String>,
    val entries: List<SessionEntry>,
    val atMillis: Long,
) {
    val partText: String
        get() = parts.joinToString(" + ")

    val totalDone: Int
        get() = entries.sumOf { it.done }

    val totalPlanned: Int
        get() = entries.sumOf { it.sets }

    /** 这次训练的总容量（kg），没记重量时为 0。 */
    val totalVolume: Double
        get() = entries.sumOf { it.volumeKg }

    val date: LocalDate
        get() = LocalDate.ofEpochDay(epochDay)

    val dateText: String
        get() = date.format(FMT)

    val summaryText: String
        get() {
            val base = "$partText · 完成 $totalDone/$totalPlanned 组"
            return if (totalVolume > 0.0) "$base · 容量 ${formatVolume(totalVolume)}" else base
        }

    companion object {
        private val FMT = DateTimeFormatter.ofPattern("yyyy/M/d", Locale.CHINA)
    }
}

/** 训练三大类：把历史记录分成各自的进步线。 */
enum class SessionCategory(
    val title: String,
    val subtitle: String,
    internal val keywords: List<String>,
) {
    PUSH("推", "胸 + 三头 + 肩前中束", listOf("胸", "三头", "肩前")),
    PULL("拉", "背 + 二头 + 肩中后束", listOf("背", "二头", "肩后")),
    LEGS("臀腿", "臀腿 + 肩中束", listOf("腿", "臀")),
    OTHER("其他", "没有归入三大类", emptyList()),
}

/** 这次训练属于哪一类：按部位关键词匹配，推 → 拉 → 臀腿，都不沾边就归「其他」。 */
fun sessionCategory(session: WorkoutSession): SessionCategory {
    val parts = session.parts
    return SessionCategory.values().firstOrNull { category ->
        category.keywords.any { keyword -> parts.any { it.contains(keyword, ignoreCase = true) } }
    } ?: SessionCategory.OTHER
}

/** 某一类训练的最近几次记录（按时间正序，方便看走势）与相对上一次的变化。 */
data class CategoryProgress(
    val category: SessionCategory,
    val sessions: List<WorkoutSession>,
) {
    val latest: WorkoutSession? get() = sessions.lastOrNull()
    val previous: WorkoutSession? get() = sessions.dropLast(1).lastOrNull()

    val volumeDelta: Double
        get() = (latest?.totalVolume ?: 0.0) - (previous?.totalVolume ?: 0.0)

    val doneDelta: Int
        get() = (latest?.totalDone ?: 0) - (previous?.totalDone ?: 0)

    val deltaText: String
        get() = when {
            latest == null -> "还没有记录"
            previous == null -> "首次记录"
            volumeDelta > 0.0 -> "+${formatVolume(volumeDelta)}"
            volumeDelta < 0.0 -> "-${formatVolume(-volumeDelta)}"
            doneDelta > 0 -> "+$doneDelta 组"
            doneDelta < 0 -> "$doneDelta 组"
            else -> "持平"
        }

    /** "3200 → 3500 → 3800" 这样的容量走势；没有重量的记录显示 —。 */
    val trendText: String
        get() = sessions.joinToString(" → ") { formatVolume(it.totalVolume) }
}

/** 取某一类最近的 [limit] 次训练。 */
fun categoryProgress(
    sessions: List<WorkoutSession>,
    category: SessionCategory,
    limit: Int = 6,
): CategoryProgress {
    val list = sessions
        .filter { sessionCategory(it) == category }
        .sortedBy { it.epochDay * 100_000 + it.atMillis % 100_000 }
        .takeLast(limit)
    return CategoryProgress(category, list)
}

/** 一天里某个动作的汇总（对比用，同名不同重量合并成一行）。 */
data class DayTotals(
    val sets: Int,
    val done: Int,
    val volume: Double,
    val repsText: String,
    val weightText: String,
) {
    val cellText: String
        get() = if (sets == 0) "—" else "完成 $done/$sets 组 · $repsText · $weightText"
}

/** 两天对比里的一行：同一个动作名在两天各自的表现。 */
data class CompareRow(
    val name: String,
    val color: Int,
    val a: DayTotals,
    val b: DayTotals,
) {
    val hasA: Boolean get() = a.sets > 0
    val hasB: Boolean get() = b.sets > 0
    val delta: Double get() = b.volume - a.volume

    /** 第二天的容量相对第一天怎么变。 */
    val deltaText: String
        get() = when {
            !hasA -> "新增 ${formatVolume(b.volume)}"
            !hasB -> "本日没有"
            delta == 0.0 -> "持平"
            delta > 0 -> "+${formatVolume(delta)}"
            else -> "-${formatVolume(-delta)}"
        }
}

/** 可保存的训练模板：名字 + 部位 + 动作清单，随时导进今天的训练再微调。 */
data class WorkoutTemplate(
    val id: Long,
    val name: String,
    val parts: List<String> = emptyList(),
    val exercises: List<Exercise> = emptyList(),
) {
    val summaryText: String
        get() {
            val partText = if (parts.isEmpty()) "未标部位" else parts.joinToString(" + ")
            val sets = exercises.sumOf { it.sets }
            return "$partText · ${exercises.size} 个动作 · $sets 组"
        }
}

/** 可选部位，按"大肌群 → 小肌群 → 有氧"的顺序排（第一次打开时的默认值，之后用户可以自己加）。 */
object WorkoutParts {
    val ALL = listOf("胸", "背", "肩", "腿", "手臂", "核心", "有氧")
}

/** 内置计划：第一次打开就有一套可以直接练的模板。 */
object DefaultPlans {

    fun all(): List<WorkoutPlan> = listOf(
        plan("chest", "胸", "胸 日",
            Exercise("卧推", 4, "8", 60.0),
            Exercise("上斜哑铃卧推", 3, "10", 24.0),
            Exercise("双杠臂屈伸", 3, "10"),
            Exercise("蝴蝶机夹胸", 3, "12", 30.0),
        ),
        plan("back", "背", "背 日",
            Exercise("引体向上", 4, "8"),
            Exercise("杠铃划船", 4, "10", 50.0),
            Exercise("高位下拉", 3, "12", 45.0),
            Exercise("单臂哑铃划船", 3, "12", 18.0),
        ),
        plan("shoulder", "肩", "肩 日",
            Exercise("站姿推举", 4, "8", 30.0),
            Exercise("哑铃侧平举", 4, "15", 8.0),
            Exercise("反向飞鸟", 3, "15", 6.0),
            Exercise("面拉", 3, "15", 20.0),
        ),
        plan("legs", "腿", "腿 日",
            Exercise("深蹲", 5, "5", 80.0),
            Exercise("罗马尼亚硬拉", 4, "8", 60.0),
            Exercise("腿举", 3, "12", 100.0),
            Exercise("站姿提踵", 4, "15", 40.0),
        ),
        plan("arms", "手臂", "手臂 日",
            Exercise("二头弯举", 3, "12", 10.0),
            Exercise("锤式弯举", 3, "12", 10.0),
            Exercise("绳索下压", 3, "12", 25.0),
            Exercise("窄距卧推", 3, "10", 40.0),
        ),
        plan("core", "核心", "核心 日",
            Exercise("卷腹", 3, "20"),
            Exercise("平板支撑", 3, "45 秒"),
            Exercise("悬垂举腿", 3, "12"),
            Exercise("俄罗斯转体", 3, "20", 5.0),
        ),
        plan("cardio", "有氧", "有氧 日",
            Exercise("慢跑", 1, "30 分钟"),
            Exercise("动感单车", 1, "30 分钟"),
            Exercise("跳绳", 3, "3 分钟"),
            Exercise("划船机", 1, "20 分钟"),
        ),
    )

    private fun plan(id: String, part: String, title: String, vararg exercises: Exercise) =
        WorkoutPlan(id = "builtin_$id", part = part, title = title, exercises = exercises.toList())
}

/** 纯逻辑：都是不依赖 Android 的函数，方便单元测试。 */
object WorkoutLogic {

    /** 按选中的部位挑出对应计划，顺序跟用户选的顺序一致。 */
    fun matchPlans(plans: List<WorkoutPlan>, parts: Collection<String>): List<WorkoutPlan> {
        val order = parts.toList()
        return order.mapNotNull { part -> plans.firstOrNull { it.part == part } }
    }

    /**
     * 多个部位合并成一份今日清单。
     *
     * 只有「名称 + 次数 + 重量」完全相同的动作才合并（组数取大的那边）；
     * 同名但次数或重量不同的动作各自保留，全部出现在今日清单里。
     */
    fun mergeExercises(plans: List<WorkoutPlan>): List<Exercise> {
        val merged = LinkedHashMap<String, Exercise>()
        plans.forEach { plan ->
            plan.exercises.forEach { e ->
                val old = merged[e.key]
                merged[e.key] = if (old == null) {
                    e
                } else {
                    val bigger = if (e.sets >= old.sets) e else old
                    old.copy(
                        sets = bigger.sets,
                        reps = bigger.reps,
                        weight = bigger.weight,
                        color = if (old.color != 0) old.color else e.color,
                    )
                }
            }
        }
        return merged.values.toList()
    }

    /** 动作库：所有计划里出现过的动作，按「名称 + 次数 + 重量」去重，按名称排序。 */
    fun exerciseLibrary(plans: List<WorkoutPlan>): List<Exercise> =
        mergeExercises(plans).sortedWith(
            compareBy<Exercise> { it.name.trim().lowercase() }
                .thenBy { it.sets }
                .thenBy { it.weight },
        )

    // ---------------- 同名动作的 N 选一（①②③ 编号） ----------------

    /**
     * 同名动作的多个「组数 / 次数 / 重量」变体组成一个 N 选一组。
     * 只返回变体数 ≥ 2 的名字（只有一个变体的动作不需要选）。
     */
    fun variantGroups(exercises: List<Exercise>): List<Pair<String, List<Exercise>>> =
        exercises.groupBy { it.name.trim().lowercase() }
            .filter { it.value.size > 1 }
            .map { (name, list) -> name to list }
            .sortedBy { it.first }

    /**
     * 每个名字选一个变体（[selected] 是 小写名字 → 变体序号），没选过默认第一个。
     * 单变体的动作全部保留；顺序不变。
     */
    fun pickVariants(
        exercises: List<Exercise>,
        selected: Map<String, Int> = emptyMap(),
    ): List<Exercise> {
        val groups = exercises.groupBy { it.name.trim().lowercase() }
        val chosen = groups.mapValues { (name, list) ->
            (selected[name] ?: 0).coerceIn(0, list.size - 1)
        }
        val seen = mutableSetOf<String>()
        return exercises.filter { e ->
            val name = e.name.trim().lowercase()
            val list = groups[name].orEmpty()
            if (list.size <= 1) {
                true
            } else {
                val index = list.indexOf(e)
                index == chosen[name] && seen.add(name)
            }
        }
    }

    /** 由动作生成条目（按 N 选一的结果），尽量保留之前已经点掉的组数。 */
    fun entriesOfVariants(
        exercises: List<Exercise>,
        previous: List<SessionEntry> = emptyList(),
        selected: Map<String, Int> = emptyMap(),
    ): List<SessionEntry> = entriesOf(pickVariants(exercises, selected), previous)

    /**
     * 同颜色的动作组成一组「N 选一」，**按动作名分选项**（同名的不同重量算同一个选项）。
     * 只返回不同名字 ≥2 的组（只有一条动作、或者同名的几个重量变体，都不需要选）。
     */
    fun colorGroups(exercises: List<Exercise>): List<Pair<Int, List<String>>> =
        exercises.filter { it.color != 0 }
            .groupBy { it.color }
            .map { (color, list) ->
                color to list.map { it.name.trim() }.distinctBy { it.lowercase() }
            }
            .filter { it.second.size > 1 }

    /**
     * 颜色组里只保留选中的那个**动作名**（[selected] 是 颜色 → 动作名）；没选过就默认组内第一个。
     * 选中名字后，该名字下不同重量/次数的动作**全部保留**；没标颜色的动作也全部保留，顺序不变。
     */
    fun pickAlternatives(
        exercises: List<Exercise>,
        selected: Map<Int, String> = emptyMap(),
    ): List<Exercise> {
        val chosenName = mutableMapOf<Int, String>()
        exercises.filter { it.color != 0 }.forEach { ex ->
            val want = selected[ex.color]?.trim()?.lowercase()
            if (chosenName[ex.color] == null) {
                val exists = want != null && exercises.any {
                    it.color == ex.color && it.name.trim().lowercase() == want
                }
                chosenName[ex.color] = if (exists) want!! else ex.name.trim().lowercase()
            }
        }
        return exercises.filter {
            it.color == 0 || chosenName[it.color] == it.name.trim().lowercase()
        }
    }

    /** 由动作生成条目，尽量保留之前已经点掉的组数（改部位/改计划时不丢进度）。 */
    fun entriesOf(
        exercises: List<Exercise>,
        previous: List<SessionEntry> = emptyList(),
        selected: Map<Int, String> = emptyMap(),
    ): List<SessionEntry> = buildEntries(pickAlternatives(exercises, selected), previous)

    /** 把一组动作转成今日条目，尽量保留相同 key / 唯一同名动作已经点掉的组数。 */
    private fun buildEntries(
        chosen: List<Exercise>,
        previous: List<SessionEntry>,
    ): List<SessionEntry> {
        val byKey = previous.associateBy { it.key }
        // 同名但次数/重量改过的动作，退一步按名字找进度，避免一改重量就清零；
        // 但如果之前有多个同名动作（不同重量各自记进度），就不猜，直接清零。
        val byName = previous.groupBy { it.name.trim().lowercase() }
        return chosen.map { e ->
            val exact = byKey[e.key]
            val fallback = if (exact == null) {
                byName[e.name.trim().lowercase()]?.takeIf { it.size == 1 }?.first()
            } else {
                null
            }
            SessionEntry(
                name = e.name,
                sets = e.sets,
                reps = e.reps,
                doneSets = (exact ?: fallback)?.done ?: 0,
                weight = e.weight,
                color = e.color,
            )
        }
    }

    // ---------------- 不同动作的 N 选一（①②③ 编号） ----------------

    /**
     * 标了同一颜色的动作组成一组「N 选一」，选项是**不同的动作**（同名不同重量只算一个选项）。
     * 只返回不同动作数 ≥ 2 的组，按颜色号排序。
     */
    fun altGroups(exercises: List<Exercise>): List<Pair<Int, List<Exercise>>> =
        exercises.filter { it.color != 0 }
            .groupBy { it.color }
            .map { (color, list) -> color to list.distinctBy { it.name.trim().lowercase() } }
            .filter { it.second.size > 1 }
            .sortedBy { it.first }

    /**
     * 每组只保留用户选的那一个动作（[selected] 是 颜色 → 组内序号），没选过默认第一个；
     * 没标颜色的动作全部保留，顺序不变。同名不同重量的动作一起保留/一起去掉。
     */
    fun pickOnePerGroup(
        exercises: List<Exercise>,
        selected: Map<Int, Int> = emptyMap(),
    ): List<Exercise> {
        val chosenName = altGroups(exercises).associate { (color, list) ->
            val index = (selected[color] ?: 0).coerceIn(0, list.size - 1)
            color to list[index].name.trim().lowercase()
        }
        return exercises.filter {
            it.color == 0 || it.name.trim().lowercase() == chosenName[it.color]
        }
    }

    /** 每个动作的备选组编号（组 1 → ①、组 2 → ②…）；没分组的动作不在结果里。 */
    fun groupMarks(exercises: List<Exercise>): Map<String, String> {
        val marks = mutableMapOf<String, String>()
        altGroups(exercises).forEach { (group, list) ->
            val mark = variantMark(group - 1)
            list.forEach { exercise ->
                marks[exercise.name.trim().lowercase()] = mark
            }
        }
        return marks
    }

    /** 由动作生成条目（按「每组选一个」的结果），尽量保留之前的组数。 */
    fun entriesOfPicks(
        exercises: List<Exercise>,
        previous: List<SessionEntry> = emptyList(),
        selected: Map<Int, Int> = emptyMap(),
    ): List<SessionEntry> = buildEntries(pickOnePerGroup(exercises, selected), previous)

    /** 把一天的条目按动作名汇总（同名不同重量合并），对比时用来对齐两天。 */
    fun dayTotals(entries: List<SessionEntry>): Map<String, DayTotals> =
        entries.groupBy { it.name.trim().lowercase() }.mapValues { (_, list) ->
            val reps = list.map { it.reps.trim() }.filter { it.isNotEmpty() }.distinct()
            val weights = list.map { it.weight }.filter { it > 0.0 }.distinct().sorted()
            DayTotals(
                sets = list.sumOf { it.sets },
                done = list.sumOf { it.done },
                volume = list.sumOf { it.volumeKg },
                repsText = if (reps.isEmpty()) "—" else reps.joinToString("/") + "次",
                weightText = when {
                    weights.isEmpty() -> "自重"
                    weights.size == 1 -> formatWeight(weights[0])
                    else -> weights.joinToString("/") { formatWeight(it) }
                },
            )
        }

    /**
     * 两天对比：按动作名对齐，两天的并集全部列出（只有一天有的也列，另一边显示 —）。
     * 排序按容量变化绝对值从大到小，变化一样时按名字。
     */
    fun compareSessions(a: WorkoutSession?, b: WorkoutSession?): List<CompareRow> {
        val aMap = dayTotals(a?.entries.orEmpty())
        val bMap = dayTotals(b?.entries.orEmpty())
        val colorOf = mutableMapOf<String, Int>()
        val displayOf = mutableMapOf<String, String>()
        (a?.entries.orEmpty() + b?.entries.orEmpty()).forEach { entry ->
            val key = entry.name.trim().lowercase()
            displayOf.putIfAbsent(key, entry.name.trim())
            if ((colorOf[key] ?: 0) == 0 && entry.color != 0) colorOf[key] = entry.color
        }
        val empty = DayTotals(0, 0, 0.0, "—", "—")
        return (aMap.keys + bMap.keys).distinct().map { key ->
            CompareRow(
                name = displayOf[key] ?: key,
                color = colorOf[key] ?: 0,
                a = aMap[key] ?: empty,
                b = bMap[key] ?: empty,
            )
        }.sortedWith(
            compareByDescending<CompareRow> { kotlin.math.abs(it.delta) }
                .thenBy { it.name.lowercase() },
        )
    }
    /** 今天就这个动作用多重：自己改的重量只影响今天的清单，不改计划。 */
    fun setWeight(entry: SessionEntry, weight: Double): SessionEntry =
        entry.copy(weight = weight.coerceIn(0.0, 1000.0))

    /**
     * 把一个动作拆成两条记录，总组数不变：前一半用 [firstSets] 组，剩下的归第二条。
     * 用于「先做 3 组 8 次 60kg，再做 2 组 6 次 70kg」这种同一动作不同负荷。
     */
    fun splitExercise(
        exercise: Exercise,
        firstSets: Int,
        firstReps: String,
        firstWeight: Double,
        secondReps: String,
        secondWeight: Double,
    ): List<Exercise> {
        val total = exercise.sets.coerceIn(2, 20)
        val first = firstSets.coerceIn(1, total - 1)
        return listOf(
            exercise.copy(sets = first, reps = firstReps.trim().ifEmpty { exercise.reps }, weight = firstWeight.coerceIn(0.0, 1000.0)),
            exercise.copy(
                sets = total - first,
                reps = secondReps.trim().ifEmpty { exercise.reps },
                weight = secondWeight.coerceIn(0.0, 1000.0),
            ),
        )
    }

    /** 把两条同名的动作合并回一条：组数相加，次数/重量取合并时选定的那组。 */
    fun mergePair(
        a: Exercise,
        b: Exercise,
        reps: String,
        weight: Double,
        color: Int = if (a.color != 0) a.color else b.color,
    ): Exercise = a.copy(
        sets = (a.sets + b.sets).coerceIn(1, 20),
        reps = reps.trim().ifEmpty { a.reps },
        weight = weight.coerceIn(0.0, 1000.0),
        color = color,
    )

    /** 调整顺序：把 [from] 位置的动作挪到 [to]（越界会被夹住）。 */
    fun <T> moveItem(list: List<T>, from: Int, to: Int): List<T> {
        if (from == to || from !in list.indices || to !in list.indices) return list
        val mutable = list.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        return mutable
    }


    /** 今日已完成的训练容量（组 × 次 × 重量）。 */
    fun totalVolume(entries: List<SessionEntry>): Double = entries.sumOf { it.volumeKg }

    /** 单击记一组：到上限后不再增加。 */
    fun addSet(entry: SessionEntry): SessionEntry =
        if (entry.done >= entry.sets) entry else entry.copy(doneSets = entry.done + 1)

    fun resetSets(entry: SessionEntry): SessionEntry = entry.copy(doneSets = 0)

    fun totalDone(entries: List<SessionEntry>): Int = entries.sumOf { it.done }

    fun totalPlanned(entries: List<SessionEntry>): Int = entries.sumOf { it.sets }

    /** 关键词 + 时间范围筛选训练记录（部位、动作名都能搜）。 */
    fun filterSessions(
        sessions: List<WorkoutSession>,
        keyword: String,
        fromEpochDay: Long = Long.MIN_VALUE,
        toEpochDay: Long = Long.MAX_VALUE,
    ): List<WorkoutSession> {
        val q = keyword.trim()
        return sessions.filter { s ->
            val inRange = (fromEpochDay == Long.MIN_VALUE || s.epochDay >= fromEpochDay) &&
                (toEpochDay == Long.MAX_VALUE || s.epochDay <= toEpochDay)
            val hit = q.isEmpty() ||
                s.parts.any { it.contains(q, ignoreCase = true) } ||
                s.entries.any { it.name.contains(q, ignoreCase = true) }
            inRange && hit
        }.sortedByDescending { it.epochDay * 100_000 + (it.atMillis % 100_000) }
    }

    /** 按日期倒序分组。 */
    fun groupSessionsByDate(sessions: List<WorkoutSession>): List<Pair<LocalDate, List<WorkoutSession>>> =
        sessions.groupBy { it.epochDay }
            .toSortedMap(reverseOrder())
            .map { (day, list) -> LocalDate.ofEpochDay(day) to list }

    // ---------------- 自定义部位 ----------------

    fun hasPart(parts: List<String>, name: String): Boolean =
        parts.any { it.equals(name.trim(), ignoreCase = true) }

    /** 新增部位；重名或空名返回原列表。 */
    fun addPart(parts: List<String>, name: String): List<String> {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || hasPart(parts, trimmed)) return parts
        return parts + trimmed
    }

    /** 改名；新名和别的部位重名时返回 null 表示改不了。 */
    fun renamePart(parts: List<String>, old: String, new: String): List<String>? {
        val trimmed = new.trim()
        if (trimmed.isEmpty()) return null
        val others = parts.filterNot { it == old }
        if (hasPart(others, trimmed)) return null
        return parts.map { if (it == old) trimmed else it }
    }

    fun removePart(parts: List<String>, name: String): List<String> =
        parts.filterNot { it == name }

    // ---------------- 模板 ----------------

    /** 把今天的动作存成模板（部位和动作都带上）。 */
    fun templateFrom(
        id: Long,
        name: String,
        parts: List<String>,
        entries: List<SessionEntry>,
    ): WorkoutTemplate =
        WorkoutTemplate(
            id = id,
            name = name.trim(),
            parts = parts,
            exercises = entries.map { Exercise(it.name, it.sets, it.reps, it.weight) },
        )

    /** 导入模板：按动作名保留已经点掉的组数，方便接着练。 */
    fun applyTemplate(template: WorkoutTemplate, previous: List<SessionEntry> = emptyList()): List<SessionEntry> =
        entriesOf(template.exercises, previous)

    /** 加一个自定义动作；同名则改它的组数/次数（已点掉的组数保留，超出新组数就截断）。 */
    fun withExercise(entries: List<SessionEntry>, exercise: Exercise): List<SessionEntry> {
        val name = exercise.name.trim()
        if (name.isEmpty()) return entries
        val index = entries.indexOfFirst { it.key == exercise.key }
        return if (index >= 0) {
            val old = entries[index]
            val updated = SessionEntry(
                name = old.name,
                sets = exercise.sets.coerceAtLeast(1),
                reps = exercise.reps,
                doneSets = old.doneSets.coerceIn(0, exercise.sets.coerceAtLeast(1)),
                weight = if (exercise.weight > 0.0) exercise.weight else old.weight,
                color = exercise.color,
            )
            entries.toMutableList().also { it[index] = updated }
        } else {
            entries + SessionEntry(
                name = name,
                sets = exercise.sets.coerceAtLeast(1),
                reps = exercise.reps,
                weight = exercise.weight,
                color = exercise.color,
            )
        }
    }

    /** 按唯一标识（名称+次数+重量）移除今天的某个动作。 */
    fun removeExerciseByKey(entries: List<SessionEntry>, key: String): List<SessionEntry> =
        entries.filterNot { it.key == key }

    fun removeExercise(entries: List<SessionEntry>, name: String): List<SessionEntry> =
        entries.filterNot { it.name == name }
}
