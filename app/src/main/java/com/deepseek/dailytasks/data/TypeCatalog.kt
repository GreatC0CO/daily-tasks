package com.deepseek.dailytasks.data

/**
 * 消耗品类型目录：把"用户新建过的类型"和"历史记录里用过的类型"合并成一个可选列表，
 * 这样新建类型之后马上就能在下拉里选到，不会因为条数太多而放不下。
 */
object TypeCatalog {

    fun normalize(name: String): String = name.trim().replace(Regex("\\s+"), " ")

    /** 合并两个来源，去重（忽略大小写），保留顺序。 */
    fun merge(stored: List<String>, history: List<String>): List<String> {
        val out = ArrayList<String>()
        fun push(raw: String) {
            val name = normalize(raw)
            if (name.isEmpty()) return
            if (out.any { it.equals(name, ignoreCase = true) }) return
            out.add(name)
        }
        stored.forEach(::push)
        history.forEach(::push)
        return out
    }

    fun contains(types: List<String>, name: String): Boolean {
        val target = normalize(name)
        if (target.isEmpty()) return false
        return types.any { normalize(it).equals(target, ignoreCase = true) }
    }

    /** 返回加入新类型后的列表；已存在（忽略大小写）则原样返回。 */
    fun add(types: List<String>, name: String): List<String> {
        val merged = merge(types, emptyList())
        val target = normalize(name)
        if (target.isEmpty() || contains(merged, target)) return merged
        return merged + target
    }

    /** 关键词过滤下拉选项。 */
    fun filter(types: List<String>, query: String): List<String> {
        val q = normalize(query)
        if (q.isEmpty()) return types
        return types.filter { it.contains(q, ignoreCase = true) }
    }
}
