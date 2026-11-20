package com.deepseek.dailytasks.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/**
 * 锻炼相关的本地存储：
 *  - 训练计划（第一次读时用内置模板，改过之后存自己的版本）
 *  - 今日训练草稿（选了什么部位、每组点了没有）
 *  - 已完成训练记录
 */
class WorkoutStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ---------------- 计划 ----------------

    fun loadPlans(): List<WorkoutPlan> {
        val raw = prefs.getString(KEY_PLANS, null) ?: return DefaultPlans.all()
        val parsed = runCatching { plansFromJson(raw) }.getOrDefault(emptyList())
        return parsed.ifEmpty { DefaultPlans.all() }
    }

    fun savePlans(plans: List<WorkoutPlan>) {
        prefs.edit().putString(KEY_PLANS, plansToJson(plans)).apply()
    }

    /** 改某个部位的计划：不动其它部位。 */
    fun savePlan(plan: WorkoutPlan) {
        val plans = loadPlans().toMutableList()
        val index = plans.indexOfFirst { it.part == plan.part }
        if (index >= 0) plans[index] = plan else plans.add(plan)
        savePlans(plans)
    }

    // ---------------- 自定义部位 ----------------

    /** 部位列表：用户自己维护，第一次打开是内置的七个。 */
    fun loadParts(): List<String> {
        val raw = prefs.getString(KEY_PARTS, null) ?: return WorkoutParts.ALL
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i)
                    if (s.isNotBlank()) add(s)
                }
            }.ifEmpty { WorkoutParts.ALL }
        }.getOrDefault(WorkoutParts.ALL)
    }

    fun saveParts(parts: List<String>) {
        val arr = JSONArray()
        parts.forEach { arr.put(it) }
        prefs.edit().putString(KEY_PARTS, arr.toString()).apply()
    }

    /** 新增部位；重名返回 false。 */
    fun addPart(name: String): Boolean {
        val parts = loadParts()
        val next = WorkoutLogic.addPart(parts, name)
        if (next == parts) return false
        saveParts(next)
        // 新部位顺手建一份空计划，之后可以直接往里加动作
        if (loadPlans().none { it.part == next.last() }) {
            savePlan(WorkoutPlan(id = "custom_" + System.currentTimeMillis(), part = next.last(), title = "${next.last()} 日", exercises = emptyList(), custom = true))
        }
        return true
    }

    fun renamePart(old: String, new: String): Boolean {
        val parts = loadParts()
        val next = WorkoutLogic.renamePart(parts, old, new) ?: return false
        saveParts(next)
        savePlans(loadPlans().map { if (it.part == old) it.copy(part = new, title = it.title.replace(old, new)) else it })
        return true
    }

    fun removePart(name: String) {
        saveParts(loadParts().filterNot { it == name })
        savePlans(loadPlans().filterNot { it.part == name })
    }

    // ---------------- 训练模板 ----------------

    fun loadTemplates(): List<WorkoutTemplate> {
        val raw = prefs.getString(KEY_TEMPLATES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val name = o.optString("name")
                    if (name.isBlank()) continue
                    add(
                        WorkoutTemplate(
                            id = o.optLong("id"),
                            name = name,
                            parts = o.optJSONArray("parts").toStringList(),
                            exercises = readExercises(o.optJSONArray("exercises")),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    /** 保存模板：同 id 覆盖，同名（忽略大小写）覆盖。 */
    fun saveTemplate(template: WorkoutTemplate) {
        val list = loadTemplates().filterNot {
            it.id == template.id || it.name.equals(template.name.trim(), ignoreCase = true)
        } + template
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("parts", JSONArray().apply { t.parts.forEach { put(it) } })
                    put("exercises", exercisesToJson(t.exercises))
                }
            )
        }
        prefs.edit().putString(KEY_TEMPLATES, arr.toString()).apply()
    }

    fun deleteTemplate(id: Long) {
        val list = loadTemplates().filterNot { it.id == id }
        val arr = JSONArray()
        list.forEach { t ->
            arr.put(
                JSONObject().apply {
                    put("id", t.id)
                    put("name", t.name)
                    put("parts", JSONArray().apply { t.parts.forEach { put(it) } })
                    put("exercises", exercisesToJson(t.exercises))
                }
            )
        }
        prefs.edit().putString(KEY_TEMPLATES, arr.toString()).apply()
    }

    // ---------------- 今日草稿 ----------------

    fun loadDraft(today: LocalDate = LocalDate.now()): WorkoutDraft? {
        val raw = prefs.getString(KEY_DRAFT, null) ?: return null
        return runCatching {
            val o = JSONObject(raw)
            val day = o.optLong("epochDay", -1L)
            // 不是今天的草稿就当没有（每天重新开始）
            if (day != today.toEpochDay()) return@runCatching null
            val parts = o.optJSONArray("parts").toStringList()
            WorkoutDraft(day, parts, readEntries(o.optJSONArray("entries")))
        }.getOrNull()
    }

    fun saveDraft(draft: WorkoutDraft) {
        val entries = JSONArray()
        draft.entries.forEach { it.putTo(entries) }
        val parts = JSONArray().apply { draft.parts.forEach { put(it) } }
        prefs.edit()
            .putString(
                KEY_DRAFT,
                JSONObject().apply {
                    put("epochDay", draft.epochDay)
                    put("parts", parts)
                    put("entries", entries)
                }.toString()
            )
            .apply()
    }

    fun clearDraft() {
        prefs.edit().remove(KEY_DRAFT).apply()
    }

    // ---------------- 训练记录 ----------------

    fun loadSessions(): List<WorkoutSession> {
        val raw = prefs.getString(KEY_SESSIONS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    add(
                        WorkoutSession(
                            id = o.optLong("id"),
                            epochDay = o.optLong("epochDay"),
                            parts = o.optJSONArray("parts").toStringList(),
                            atMillis = o.optLong("at"),
                            entries = readEntries(o.optJSONArray("entries")),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun addSession(session: WorkoutSession) {
        val list = (loadSessions() + session)
            .distinctBy { it.id }
            .sortedByDescending { it.epochDay * 100_000 + (it.atMillis % 100_000) }
        val arr = JSONArray()
        list.forEach { s ->
            val entries = JSONArray()
            s.entries.forEach { it.putTo(entries) }
            val parts = JSONArray().apply { s.parts.forEach { put(it) } }
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("epochDay", s.epochDay)
                    put("parts", parts)
                    put("at", s.atMillis)
                    put("entries", entries)
                }
            )
        }
        prefs.edit().putString(KEY_SESSIONS, arr.toString()).apply()
    }

    fun deleteSession(id: Long) {
        val list = loadSessions().filterNot { it.id == id }
        prefs.edit().putString(KEY_SESSIONS, sessionsToJson(list)).apply()
    }

    fun pruneSessions(today: LocalDate = LocalDate.now(), keepDays: Long = 365) {
        val threshold = today.minusDays(keepDays).toEpochDay()
        val list = loadSessions().filter { it.epochDay >= threshold }
        prefs.edit().putString(KEY_SESSIONS, sessionsToJson(list)).apply()
    }

    // ---------------- 今日锻炼日记 ----------------

    /** 读某天的锻炼日记；没写过返回空串。 */
    fun loadJournal(date: LocalDate): String {
        val raw = prefs.getString(KEY_JOURNALS, null) ?: return ""
        return runCatching {
            JSONObject(raw).optString(date.toEpochDay().toString())
        }.getOrDefault("")
    }

    /** 写某天的锻炼日记；写空串等于删掉这一天的那篇。 */
    fun saveJournal(date: LocalDate, text: String) {
        val trimmed = text.trim()
        val obj = runCatching {
            prefs.getString(KEY_JOURNALS, null)?.let { JSONObject(it) } ?: JSONObject()
        }.getOrDefault(JSONObject())
        val key = date.toEpochDay().toString()
        if (trimmed.isEmpty()) obj.remove(key) else obj.put(key, trimmed)
        prefs.edit().putString(KEY_JOURNALS, obj.toString()).apply()
    }

    // ---------------- 私有工具 ----------------

    private fun sessionsToJson(list: List<WorkoutSession>): String {
        val arr = JSONArray()
        list.forEach { s ->
            val entries = JSONArray()
            s.entries.forEach { it.putTo(entries) }
            val parts = JSONArray().apply { s.parts.forEach { put(it) } }
            arr.put(
                JSONObject().apply {
                    put("id", s.id)
                    put("epochDay", s.epochDay)
                    put("parts", parts)
                    put("at", s.atMillis)
                    put("entries", entries)
                }
            )
        }
        return arr.toString()
    }

    private fun exercisesToJson(list: List<Exercise>): JSONArray {
        val arr = JSONArray()
        list.forEach { e ->
            arr.put(
                JSONObject().apply {
                    put("name", e.name)
                    put("sets", e.sets)
                    put("reps", e.reps)
                    put("weight", e.weight)
                    put("color", e.color)
                }
            )
        }
        return arr
    }

    private fun readExercises(arr: JSONArray?): List<Exercise> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name")
                if (name.isBlank()) continue
                add(
                    Exercise(
                        name = name,
                        sets = o.optInt("sets", 3).coerceAtLeast(1),
                        reps = o.optString("reps", "12"),
                        weight = o.optDouble("weight", 0.0).takeIf { it.isFinite() && it > 0.0 } ?: 0.0,
                        color = o.optInt("color", 0).coerceIn(0, EXERCISE_COLORS.size),
                    )
                )
            }
        }
    }

    private fun readEntries(arr: JSONArray?): List<SessionEntry> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                val o = arr.optJSONObject(i) ?: continue
                val name = o.optString("name")
                if (name.isBlank()) continue
                val sets = o.optInt("sets", 3).coerceAtLeast(1)
                add(
                    SessionEntry(
                        name = name,
                        sets = sets,
                        reps = o.optString("reps", "12"),
                        doneSets = o.optInt("done", 0).coerceIn(0, sets),
                        weight = o.optDouble("weight", 0.0).takeIf { it.isFinite() && it > 0.0 } ?: 0.0,
                        color = o.optInt("color", 0).coerceIn(0, EXERCISE_COLORS.size),
                    )
                )
            }
        }
    }

    private fun SessionEntry.putTo(arr: JSONArray) {
        arr.put(
            JSONObject().apply {
                put("name", name)
                put("sets", sets)
                put("reps", reps)
                put("done", doneSets)
                put("weight", weight)
                put("color", color)
            }
        )
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return buildList {
            for (i in 0 until length()) {
                val s = optString(i)
                if (s.isNotBlank()) add(s)
            }
        }
    }

    private companion object {
        const val PREFS_NAME = "workouts"
        const val KEY_PLANS = "plans_json"
        const val KEY_PARTS = "parts_json"
        const val KEY_TEMPLATES = "templates_json"
        const val KEY_DRAFT = "draft_json"
        const val KEY_SESSIONS = "sessions_json"
        const val KEY_JOURNALS = "journal_json"
    }
}

/**
 * 训练计划的 JSON 编解码。
 *
 * 单独抽成文件级函数：`savePlans` / `loadPlans` 共用同一套字段，且不依赖 Context，
 * 可以直接用普通 JUnit 测「存进去什么、读出来还是什么」。
 * 注意重量 weight 必须写进来——早期版本漏了它，导致「编辑整体计划后重量回退成自重」。
 */
internal fun plansToJson(plans: List<WorkoutPlan>): String {
    val arr = JSONArray()
    plans.forEach { plan ->
        val ex = JSONArray()
        plan.exercises.forEach { e ->
            ex.put(
                JSONObject().apply {
                    put("name", e.name)
                    put("sets", e.sets)
                    put("reps", e.reps)
                    put("weight", e.weight)
                    put("color", e.color)
                }
            )
        }
        arr.put(
            JSONObject().apply {
                put("id", plan.id)
                put("part", plan.part)
                put("title", plan.title)
                put("custom", plan.custom)
                put("exercises", ex)
            }
        )
    }
    return arr.toString()
}

internal fun plansFromJson(raw: String): List<WorkoutPlan> {
    val arr = JSONArray(raw)
    return buildList {
        for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val part = o.optString("part")
            if (part.isBlank()) continue
            val exArr = o.optJSONArray("exercises")
            val exercises = buildList {
                if (exArr != null) {
                    for (j in 0 until exArr.length()) {
                        val e = exArr.optJSONObject(j) ?: continue
                        val name = e.optString("name")
                        if (name.isBlank()) continue
                        add(
                            Exercise(
                                name = name,
                                sets = e.optInt("sets", 3).coerceAtLeast(1),
                                reps = e.optString("reps", "12"),
                                weight = e.optDouble("weight", 0.0)
                                    .takeIf { it.isFinite() && it > 0.0 } ?: 0.0,
                                color = e.optInt("color", 0).coerceIn(0, EXERCISE_COLORS.size),
                            )
                        )
                    }
                }
            }
            add(
                WorkoutPlan(
                    id = o.optString("id"),
                    part = part,
                    title = o.optString("title", "$part 日"),
                    custom = o.optBoolean("custom", false),
                    exercises = exercises,
                )
            )
        }
    }
}
