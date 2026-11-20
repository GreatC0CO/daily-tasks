package com.deepseek.dailytasks.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** 消耗品记录的本地存储（SharedPreferences + JSON）。 */
class ConsumableStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun loadConsumables(): List<Consumable> {
        val raw = prefs.getString(KEY_ITEMS, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.optJSONObject(i) ?: continue
                    val name = o.optString("name")
                    if (name.isBlank()) continue
                    add(
                        Consumable(
                            id = o.optLong("id"),
                            name = name,
                            type = o.optString("type", ""),
                            startDate = parseDate(o.optString("start")) ?: LocalDate.now(),
                            endDate = parseDate(o.optString("end")),
                            quantity = o.optDouble("quantity", 1.0),
                            unit = o.optString("unit", "个").ifBlank { "个" },
                            price = o.optDouble("price", 0.0),
                            note = o.optString("note", ""),
                            totalAmount = o.optNullableDouble("total"),
                            remainingAmount = o.optNullableDouble("remaining"),
                            metricMode = o.optInt("metricMode", Consumable.MODE_DAYS)
                                .let { if (it == Consumable.MODE_AMOUNT) Consumable.MODE_AMOUNT else Consumable.MODE_DAYS },
                            priceHistory = o.optJSONArray("prices")?.let { prices ->
                                buildList {
                                    for (p in 0 until prices.length()) {
                                        val point = prices.optJSONObject(p) ?: continue
                                        val price = point.optDouble("price", -1.0)
                                        if (price >= 0.0) {
                                            add(PricePoint(point.optLong("day"), price))
                                        }
                                    }
                                }.sortedBy { it.epochDay }
                            } ?: emptyList(),
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun saveConsumables(items: List<Consumable>) {
        val arr = JSONArray()
        items.forEach { c ->
            arr.put(
                JSONObject().apply {
                    put("id", c.id)
                    put("name", c.name)
                    put("type", c.type)
                    put("start", c.startDate.toString())
                    c.endDate?.let { put("end", it.toString()) }
                    put("quantity", c.quantity)
                    put("unit", c.unit)
                    put("price", c.price)
                    put("note", c.note)
                    c.totalAmount?.let { put("total", it) }
                    c.remainingAmount?.let { put("remaining", it) }
                    put("metricMode", c.metricMode)
                    if (c.priceHistory.isNotEmpty()) {
                        val prices = JSONArray()
                        c.priceHistory.forEach { point ->
                            prices.put(JSONObject().put("day", point.epochDay).put("price", point.price))
                        }
                        put("prices", prices)
                    }
                }
            )
        }
        prefs.edit().putString(KEY_ITEMS, arr.toString()).apply()
    }

    // ---------------- 记一笔购买 ----------------

    /**
     * 记一笔购买：按名称自动归档到已有条目（同名合并，价格、次数、累计花费都算在一起）；
     * 名称没出现过就新建一条。返回归档后的那条记录。
     */
    fun recordPurchase(
        name: String,
        type: String,
        price: Double,
        amount: Double,
        unit: String,
        note: String,
        date: LocalDate,
    ): Consumable? {
        val clean = name.trim()
        if (clean.isBlank()) return null
        val items = loadConsumables()
        val index = items.indexOfFirst { NameGuard.normalize(it.name) == NameGuard.normalize(clean) }
        val saved: Consumable
        val next: List<Consumable>
        if (index >= 0) {
            saved = items[index].withPurchase(date, price, amount)
            next = items.toMutableList().also { it[index] = saved }
        } else {
            saved = Consumable(
                id = System.currentTimeMillis(),
                name = clean,
                type = type.trim(),
                startDate = date,
                quantity = amount.coerceAtLeast(0.0),
                unit = unit.ifBlank { "个" },
                price = price.coerceAtLeast(0.0),
                note = note.trim(),
                totalAmount = amount.coerceAtLeast(0.0),
                remainingAmount = amount.coerceAtLeast(0.0),
                priceHistory = if (price > 0.0) {
                    listOf(PricePoint(date.toEpochDay(), price))
                } else {
                    emptyList()
                },
            )
            next = items + saved
        }
        saveConsumables(next)
        return saved
    }

    /** 按名称找已有的那条记录（忽略大小写与多余空格）。 */
    fun findByName(name: String): Consumable? =
        loadConsumables().firstOrNull { NameGuard.normalize(it.name) == NameGuard.normalize(name) }

    // ---------------- 类型目录 ----------------

    /** 用户显式新建过的类型（不含从记录里推导出来的）。 */
    fun storedTypes(): List<String> {
        val raw = prefs.getString(KEY_TYPES, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val name = arr.optString(i)
                    if (name.isNotBlank()) add(name)
                }
            }
        }.getOrDefault(emptyList())
    }

    /** 下拉里可以选的全部类型：显式新建的 + 历史记录里用过的。 */
    fun loadTypes(): List<String> =
        TypeCatalog.merge(storedTypes(), loadConsumables().map { it.type })

    fun saveTypes(types: List<String>) {
        val arr = JSONArray()
        TypeCatalog.merge(types, emptyList()).forEach { arr.put(it) }
        prefs.edit().putString(KEY_TYPES, arr.toString()).apply()
    }

    /** 新建一个自定义类型；返回是否真的新增了（重名返回 false）。 */
    fun addType(name: String): Boolean {
        val current = loadTypes()
        if (TypeCatalog.contains(current, name)) return false
        saveTypes(TypeCatalog.add(current, name))
        return true
    }

    /** 历史类型（兼容旧调用）。 */
    fun existingTypes(): List<String> = loadTypes()

    // ---------------- 列表显示方式 ----------------

    /** 0 = 一条条罗列，1 = 按类型分模块。 */
    fun loadViewMode(): Int = prefs.getInt(KEY_VIEW_MODE, VIEW_LIST).let {
        if (it == VIEW_GROUPS) VIEW_GROUPS else VIEW_LIST
    }

    fun saveViewMode(mode: Int) {
        prefs.edit().putInt(KEY_VIEW_MODE, if (mode == VIEW_GROUPS) VIEW_GROUPS else VIEW_LIST).apply()
    }

    private fun parseDate(text: String): LocalDate? =
        if (text.isBlank()) null else runCatching { LocalDate.parse(text) }.getOrNull()

    /** JSON 里没这个键（或为 null）时返回 null，而不是 0.0。 */
    private fun JSONObject.optNullableDouble(key: String): Double? =
        if (!has(key) || isNull(key)) null else optDouble(key)

    companion object {
        /** 一条条罗列 */
        const val VIEW_LIST = 0

        /** 按类型分模块 */
        const val VIEW_GROUPS = 1

        private const val PREFS_NAME = "consumables"
        private const val KEY_ITEMS = "items_json"
        private const val KEY_TYPES = "types_json"
        private const val KEY_VIEW_MODE = "view_mode"
    }
}
