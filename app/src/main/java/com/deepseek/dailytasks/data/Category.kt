package com.deepseek.dailytasks.data

/** 用户自定义的任务分类。 */
data class Category(
    val id: String,
    val name: String,
    val colorArgb: Long,
) {
    companion object {
        /** 新建分类时循环取用的颜色。 */
        val PALETTE = listOf(
            0xFF2E6BE6L,
            0xFF2FA84FL,
            0xFFF0A020L,
            0xFFD64545L,
            0xFF8E5BEFL,
            0xFF12A0A8L,
            0xFFE0609BL,
            0xFF6E7B8BL,
        )

        /** 第一次打开 App 时自带的分类。 */
        val DEFAULTS = listOf(
            Category("work", "工作", 0xFF2E6BE6L),
            Category("life", "生活", 0xFF2FA84FL),
            Category("study", "学习", 0xFFF0A020L),
            Category("sport", "运动", 0xFFD64545L),
        )

        const val UNCATEGORIZED_NAME = "未分类"

        fun nextColor(existing: List<Category>): Long = PALETTE[existing.size % PALETTE.size]
    }
}
