package com.deepseek.dailytasks.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * 动作库的「仓库条目」：动作名 + 文字说明 + 图片（相册里的一个持久化 URI）。
 *
 * 动作本身的组数/次数/重量来自计划（[Exercise]），这里只存动作的**资料**：
 * 怎么练、注意什么、示范图。同名动作共用一条资料。
 */
data class ExerciseNote(
    val name: String,
    val detail: String = "",
    val imageUri: String = "",
) {
    val hasImage: Boolean get() = imageUri.isNotBlank()

    /** 资料摘要，列表里显示一两行。 */
    val summary: String
        get() = when {
            detail.isBlank() -> "还没有写说明"
            detail.length <= 40 -> detail
            else -> detail.take(40) + "…"
        }
}

/** 动作库资料：`notes_json` 存一个数组，每个元素 {name, detail, image}。 */
class ExerciseLibraryStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun load(): List<ExerciseNote> {
        val raw = prefs.getString(KEY_NOTES, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { i ->
                val o = array.optJSONObject(i) ?: return@mapNotNull null
                val name = o.optString("name", "").trim()
                if (name.isEmpty()) return@mapNotNull null
                ExerciseNote(
                    name = name,
                    detail = o.optString("detail", ""),
                    imageUri = o.optString("image", ""),
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(notes: List<ExerciseNote>) {
        val array = JSONArray()
        notes.filter { it.name.isNotBlank() }.forEach { note ->
            array.put(
                JSONObject()
                    .put("name", note.name)
                    .put("detail", note.detail)
                    .put("image", note.imageUri),
            )
        }
        prefs.edit().putString(KEY_NOTES, array.toString()).apply()
    }

    /** 同名动作的资料（没有就是空的）。 */
    fun noteOf(notes: List<ExerciseNote>, name: String): ExerciseNote =
        notes.firstOrNull { it.name.trim().equals(name.trim(), ignoreCase = true) }
            ?: ExerciseNote(name)

    private companion object {
        const val PREFS = "exercise_library"
        const val KEY_NOTES = "notes_json"
    }
}
