package com.deepseek.dailytasks.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 极简本地存储（SharedPreferences + JSON）：
 *  - 任务模板（含重复规则和分类）
 *  - 用户自定义分类
 *  - 每天的打勾状态，按日期分开保存，所以第二天自动是"未完成"的新一天
 */
class TaskStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------------- 任务 ----------------

    fun loadTasks(): List<Task> {
        val raw = prefs.getString(KEY_TASKS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        Task(
                            id = o.optLong("id"),
                            title = o.optString("title"),
                            startMinutes = o.optInt("start"),
                            note = o.optString("note"),
                            remind = o.optBoolean("remind", true),
                            repeatEveryDays = o.optInt("repeatEveryDays", 1),
                            anchorEpochDay = o.optLong("anchorEpochDay", 0L),
                            categoryId = o.optString("categoryId", ""),
                            finishedEpochDay = if (o.has("finishedEpochDay") && !o.isNull("finishedEpochDay")) {
                                o.optLong("finishedEpochDay")
                            } else {
                                null
                            },
                            priority = o.optInt("priority", Task.PRIORITY_NORMAL)
                                .coerceIn(Task.PRIORITY_NORMAL, Task.PRIORITY_NECESSARY),
                            skipEpochDay = o.optLong("skipEpochDay", 0L),
                            showEpochDay = o.optLong("showEpochDay", 0L),
                        )
                    )
                }
            }.sortedBy { it.startMinutes }
        }.getOrDefault(emptyList())
    }

    fun saveTasks(tasks: List<Task>) {
        val arr = JSONArray()
        tasks.sortedBy { it.startMinutes }.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("title", t.title)
                    put("start", t.startMinutes)
                    put("note", t.note)
                    put("remind", t.remind)
                    put("repeatEveryDays", t.repeatEveryDays)
                    put("anchorEpochDay", t.anchorEpochDay)
                    put("categoryId", t.categoryId)
                    put("priority", t.priority)
                    if (t.skipEpochDay != 0L) put("skipEpochDay", t.skipEpochDay)
                    if (t.showEpochDay != 0L) put("showEpochDay", t.showEpochDay)
                    t.finishedEpochDay?.let { put("finishedEpochDay", it) }
                }
            )
        }
        prefs.edit().putString(KEY_TASKS, arr.toString()).apply()
    }

    // ---------------- 分类 ----------------

    fun loadCategories(): List<Category> {
        val raw = prefs.getString(KEY_CATEGORIES, null)
            ?: return Category.DEFAULTS
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val id = o.optString("id")
                    val name = o.optString("name")
                    if (id.isBlank() || name.isBlank()) continue
                    add(Category(id, name, o.optLong("color", 0xFF2E6BE6L)))
                }
            }
        }.getOrDefault(Category.DEFAULTS)
    }

    fun saveCategories(categories: List<Category>) {
        val arr = JSONArray()
        categories.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("color", c.colorArgb)
                }
            )
        }
        prefs.edit().putString(KEY_CATEGORIES, arr.toString()).apply()
    }

    // ---------------- 每天的完成状态 ----------------

    fun loadDone(date: LocalDate): Set<Long> {
        val raw = prefs.getString(doneKey(date), null) ?: return emptySet()
        return raw.split(',')
            .mapNotNull { it.trim().toLongOrNull() }
            .toSet()
    }

    fun saveDone(date: LocalDate, ids: Set<Long>) {
        prefs.edit().putString(doneKey(date), ids.joinToString(",")).apply()
    }

    /** 只保留最近 [keepDays] 天的打卡记录，避免无限增长。 */
    fun pruneHistory(today: LocalDate = LocalDate.now(), keepDays: Long = 60) {
        val threshold = today.minusDays(keepDays)
        val editor = prefs.edit()
        var removed = false
        prefs.all.keys.forEach { key ->
            if (!key.startsWith(DONE_PREFIX)) return@forEach
            val date = runCatching { LocalDate.parse(key.removePrefix(DONE_PREFIX)) }.getOrNull()
                ?: return@forEach
            if (date.isBefore(threshold)) {
                editor.remove(key)
                removed = true
            }
        }
        if (removed) editor.apply()
    }

    private fun doneKey(date: LocalDate): String = DONE_PREFIX + date

    // ---------------- 小标语 ----------------

    /** v1.20 及更早版本的自定义标语，只在迁移到分组时读取。 */
    fun loadSlogan(): String = prefs.getString(KEY_SLOGAN, "") ?: ""

    /** v1.20 及更早版本的语句库，只在迁移到分组时读取。 */
    fun loadSloganLibrary(): List<String> {
        val raw = prefs.getString(KEY_SLOGAN_LIBRARY, null) ?: return DefaultSlogans.ALL
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i)
                    if (s.isNotBlank()) add(s)
                }
            }
        }.getOrDefault(DefaultSlogans.ALL)
    }

    /**
     * 按时段分组的小标语。没存过时：
     * 夜间 22:00–07:00 一组早睡早起，白天 07:00–22:00 用旧自定义句 + 旧语句库，
     * 老用户升级后原来的标语不会丢。
     */
    fun loadSloganGroups(): List<SloganGroup> {
        val raw = prefs.getString(KEY_SLOGAN_GROUPS, null)
            ?: return SloganLogic.defaultGroups(
                daySlogans = (listOf(loadSlogan()) + loadSloganLibrary())
                    .filter { it.isNotBlank() }
                    .distinct(),
            )
        return runCatching { SloganLogic.fromJson(raw) }
            .getOrDefault(SloganLogic.defaultGroups(DefaultSlogans.ALL))
    }

    fun saveSloganGroups(groups: List<SloganGroup>) {
        prefs.edit().putString(KEY_SLOGAN_GROUPS, SloganLogic.toJson(groups)).apply()
    }

    private companion object {
        const val PREFS_NAME = "daily_tasks"
        const val KEY_TASKS = "tasks_json"
        const val KEY_CATEGORIES = "categories_json"
        const val KEY_SLOGAN = "slogan_text"
        const val KEY_SLOGAN_LIBRARY = "slogan_library_json"
        const val KEY_SLOGAN_GROUPS = "slogan_groups_json"
        const val DONE_PREFIX = "done_"
    }
}

/** 内置小标语：第一次打开、还没编辑过语句库时用。 */
internal object DefaultSlogans {
    val ALL = listOf(
        "今天也只做最重要的几件事",
        "一件一件来，做完就很好",
        "慢一点没关系，别停下",
        "先做必要的，再做想做的",
        "把今天的勾，攒成明天的底气",
    )

    val NIGHT = listOf(
        "早点睡，明早才有力气",
        "关掉屏幕，给大脑留点安静",
        "今天到这，剩下的事明天再说",
    )
}

/**
 * 一组按时段生效的小标语。
 *
 * [startMinute] / [endMinute] 是当天分钟数（0..1439）：结束早于开始表示跨零点，
 * 两者相等表示全天。组内句子按 [rotateMinutes] 分钟轮换。
 */
data class SloganGroup(
    val id: Long,
    val startMinute: Int,
    val endMinute: Int,
    val slogans: List<String>,
    val rotateMinutes: Int = 60,
) {
    val isAllDay: Boolean get() = startMinute == endMinute

    val timeText: String
        get() = if (isAllDay) {
            "全天"
        } else {
            "${SloganLogic.formatMinute(startMinute)}–${SloganLogic.formatMinute(endMinute)}"
        }

    val rotateText: String
        get() = if (rotateMinutes > 0 && rotateMinutes % 60 == 0) {
            "${rotateMinutes / 60} 小时"
        } else {
            "${rotateMinutes.coerceAtLeast(1)} 分钟"
        }

    fun contains(minute: Int): Boolean = when {
        isAllDay -> true
        startMinute < endMinute -> minute in startMinute until endMinute
        else -> minute >= startMinute || minute < endMinute
    }
}

/** 小标语的纯逻辑：时段匹配、轮换取句、默认分组和 JSON 编解码（方便单测）。 */
object SloganLogic {

    fun minuteOfDay(time: java.time.LocalTime): Int = time.hour * 60 + time.minute

    fun formatMinute(minute: Int): String {
        val m = minute.coerceIn(0, 1439)
        return String.format(java.util.Locale.CHINA, "%02d:%02d", m / 60, m % 60)
    }

    /** 解析 "22:00" / "7:30" / "2200"；不合法返回 null。 */
    fun parseMinute(text: String): Int? {
        val raw = text.trim()
        val hour: Int
        val minute: Int
        if (raw.contains(':') || raw.contains('：')) {
            val parts = raw.split(':', '：')
            if (parts.size != 2) return null
            hour = parts[0].trim().toIntOrNull() ?: return null
            minute = parts[1].trim().toIntOrNull() ?: return null
        } else if (raw.length in 3..4 && raw.all { it.isDigit() }) {
            val digits = raw.padStart(4, '0')
            hour = digits.substring(0, 2).toIntOrNull() ?: return null
            minute = digits.substring(2, 4).toIntOrNull() ?: return null
        } else {
            return null
        }
        if (hour !in 0..23 || minute !in 0..59) return null
        return hour * 60 + minute
    }

    /** 当前时刻落在哪一组；重叠时取列表里靠前的一组。 */
    fun activeGroup(groups: List<SloganGroup>, minute: Int): SloganGroup? =
        groups.firstOrNull { it.contains(minute) }

    /** 按轮换周期取这一组当前该显示的那句；[offset] 是手动「换一句」的次数。 */
    fun sloganAt(group: SloganGroup?, epochMinute: Long, offset: Int = 0): String? {
        if (group == null || group.slogans.isEmpty()) return null
        val period = group.rotateMinutes.coerceAtLeast(1)
        val slot = Math.floorMod(epochMinute / period + offset, group.slogans.size.toLong()).toInt()
        return group.slogans[slot]
    }

    fun defaultGroups(daySlogans: List<String>): List<SloganGroup> = listOf(
        SloganGroup(
            id = 1L,
            startMinute = 22 * 60,
            endMinute = 7 * 60,
            slogans = DefaultSlogans.NIGHT,
            rotateMinutes = 30,
        ),
        SloganGroup(
            id = 2L,
            startMinute = 7 * 60,
            endMinute = 22 * 60,
            slogans = daySlogans.ifEmpty { DefaultSlogans.ALL },
            rotateMinutes = 60,
        ),
    )

    fun toJson(groups: List<SloganGroup>): String {
        val arr = JSONArray()
        groups.forEach { g ->
            arr.put(
                JSONObject().apply {
                    put("id", g.id)
                    put("start", g.startMinute)
                    put("end", g.endMinute)
                    put("rotate", g.rotateMinutes)
                    put("slogans", JSONArray().apply { g.slogans.forEach { put(it) } })
                }
            )
        }
        return arr.toString()
    }

    fun fromJson(raw: String): List<SloganGroup> {
        val arr = JSONArray(raw)
        val list = buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val slogans = o.optJSONArray("slogans").toStringList()
                add(
                    SloganGroup(
                        id = o.optLong("id", i + 1L),
                        startMinute = o.optInt("start", 0).coerceIn(0, 1439),
                        endMinute = o.optInt("end", 0).coerceIn(0, 1439),
                        slogans = slogans,
                        rotateMinutes = o.optInt("rotate", 60).coerceIn(1, 1440),
                    )
                )
            }
        }
        return list.ifEmpty { defaultGroups(DefaultSlogans.ALL) }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val s = optString(i).trim()
                if (s.isNotEmpty()) add(s)
            }
        }
    }
}
