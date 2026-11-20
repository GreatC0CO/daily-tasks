package com.deepseek.dailytasks.data

/**
 * 重名校验：任务、消耗品、消耗品类型都不允许重复（忽略大小写和多余空格）。
 */
object NameGuard {

    fun normalize(name: String): String =
        name.trim().replace(Regex("\\s+"), " ").lowercase()

    /** [name] 是否和 [others] 里已有的名字重复。 */
    fun isDuplicate(name: String, others: Iterable<String>): Boolean {
        val n = normalize(name)
        if (n.isEmpty()) return false
        return others.any { normalize(it) == n }
    }

    /** 新增/编辑任务时的重名检查，[excludeId] 用于编辑时排除自己。 */
    fun taskDuplicate(title: String, tasks: List<Task>, excludeId: Long? = null): Boolean =
        isDuplicate(title, tasks.filter { it.id != excludeId }.map { it.title })

    /** 新增/编辑消耗品时的重名检查。 */
    fun consumableDuplicate(name: String, items: List<Consumable>, excludeId: Long? = null): Boolean =
        isDuplicate(name, items.filter { it.id != excludeId }.map { it.name })
}
