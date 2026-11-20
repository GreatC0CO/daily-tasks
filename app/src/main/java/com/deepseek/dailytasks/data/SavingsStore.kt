package com.deepseek.dailytasks.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** 攒钱目标与本月必要开支的本地存储（SharedPreferences + JSON）。 */
class SavingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun loadGoals(): List<SavingGoal> = try {
        val raw = prefs.getString(KEY_GOALS, null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val o = array.optJSONObject(index) ?: return@mapNotNull null
            val contributionsJson = o.optJSONArray("contributions") ?: JSONArray()
            val contributions = (0 until contributionsJson.length())
                .mapNotNull { contributionsJson.optJSONObject(it) }
                .map { c ->
                    SavingContribution(
                        epochDay = c.optLong("day"),
                        taskId = c.optLong("taskId"),
                        taskName = c.optString("taskName"),
                        amount = c.optDouble("amount", 0.0),
                    )
                }
            val tasks = (o.optJSONArray("tasks") ?: JSONArray()).let { t ->
                (0 until t.length()).mapNotNull { t.optJSONObject(it) }.map { task ->
                    val kindText = task.optString("kind")
                    // v1.15 及以前只有 cadence：daily=每天一次，each=同一日可多次。
                    val kind = when {
                        kindText == "milestone" -> SavingTaskKind.MILESTONE
                        kindText == "daily" -> SavingTaskKind.DAILY
                        task.optString("cadence") == "each" -> SavingTaskKind.DAILY
                        else -> SavingTaskKind.DAILY
                    }
                    val oldCadence = task.optString("cadence")
                    val maxPerDay = task.optInt("maxPerDay", 0).coerceIn(0, 99)
                    SavingTask(
                        id = task.optLong("id"),
                        name = task.optString("name"),
                        amount = task.optDouble("amount", 0.0),
                        kind = kind,
                        targetCount = task.optInt("targetCount", 0).coerceIn(0, 9999),
                        // 老版本「每天一次」等价于 daily 且每天上限 1 次。
                        maxPerDay = if (oldCadence == "daily" && kind == SavingTaskKind.DAILY) 1 else maxPerDay,
                    )
                }
            }
            SavingGoal(
                id = o.optLong("id"),
                name = o.optString("name"),
                targetAmount = o.optDouble("target", 0.0),
                savedAmount = contributions.sumOf { it.amount },
                note = o.optString("note"),
                tasks = tasks,
                contributions = contributions,
            )
        }.filter { it.name.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveGoals(goals: List<SavingGoal>) {
        val array = JSONArray()
        goals.forEach { goal ->
            val tasks = JSONArray()
            goal.tasks.forEach { task ->
                tasks.put(
                    JSONObject()
                        .put("id", task.id)
                        .put("name", task.name)
                        .put("amount", task.amount)
                        .put("kind", if (task.kind == SavingTaskKind.MILESTONE) "milestone" else "daily")
                        .put("targetCount", task.targetCount)
                        .put("maxPerDay", task.maxPerDay),
                )
            }
            val contributions = JSONArray()
            goal.contributions.forEach { c ->
                contributions.put(
                    JSONObject()
                        .put("day", c.epochDay)
                        .put("taskId", c.taskId)
                        .put("taskName", c.taskName)
                        .put("amount", c.amount),
                )
            }
            array.put(
                JSONObject()
                    .put("id", goal.id)
                    .put("name", goal.name)
                    .put("target", goal.targetAmount)
                    .put("saved", goal.savedAmount)
                    .put("note", goal.note)
                    .put("tasks", tasks)
                    .put("contributions", contributions),
            )
        }
        prefs.edit().putString(KEY_GOALS, array.toString()).apply()
    }

    fun loadBudget(): List<BudgetItem> = try {
        val raw = prefs.getString(KEY_BUDGET, null) ?: return emptyList()
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val o = array.optJSONObject(index) ?: return@mapNotNull null
            BudgetItem(
                id = o.optLong("id"),
                name = o.optString("name"),
                amount = o.optDouble("amount", 0.0),
                note = o.optString("note"),
                reservedMonth = o.optString("reservedMonth"),
                decreaseAmount = o.optDouble("decreaseAmount", 0.0),
                decreaseEveryDays = o.optInt("decreaseEveryDays", 1).coerceIn(1, 30),
                decreaseStartEpochDay = o.optLong("decreaseStart", 0L),
            )
        }.filter { it.name.isNotBlank() }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveBudget(items: List<BudgetItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject()
                    .put("id", item.id)
                    .put("name", item.name)
                    .put("amount", item.amount)
                    .put("note", item.note)
                    .put("reservedMonth", item.reservedMonth)
                    .put("decreaseAmount", item.decreaseAmount)
                    .put("decreaseEveryDays", item.decreaseEveryDays)
                    .put("decreaseStart", item.decreaseStartEpochDay),
            )
        }
        prefs.edit().putString(KEY_BUDGET, array.toString()).apply()
    }

    private companion object {
        const val PREFS = "savings"
        const val KEY_GOALS = "goals_json"
        const val KEY_BUDGET = "budget_json"
    }
}
