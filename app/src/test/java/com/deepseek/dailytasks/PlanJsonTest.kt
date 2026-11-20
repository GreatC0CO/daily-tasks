package com.deepseek.dailytasks

import com.deepseek.dailytasks.data.Exercise
import com.deepseek.dailytasks.data.WorkoutPlan
import com.deepseek.dailytasks.data.plansFromJson
import com.deepseek.dailytasks.data.plansToJson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 回归测试：计划 JSON 往返必须保留重量。
 * 之前 savePlans 漏写 weight，导致「编辑整体计划 → 保存 → 重量退回自重」。
 */
class PlanJsonTest {

    private fun plan(vararg exercises: Exercise) = WorkoutPlan(
        id = "p1",
        part = "胸",
        title = "胸 日",
        custom = false,
        exercises = exercises.toList(),
    )

    @Test
    fun weightSurvivesPlanJsonRoundTrip() {
        val original = plan(
            Exercise("卧推", 4, "8", 60.0),
            Exercise("俯卧撑", 3, "15", 0.0),
            Exercise("哑铃飞鸟", 3, "12", 7.5),
        )
        val decoded = plansFromJson(plansToJson(listOf(original)))

        assertEquals(1, decoded.size)
        val exercises = decoded[0].exercises
        assertEquals(3, exercises.size)
        assertEquals(60.0, exercises[0].weight, 1e-9)
        assertEquals(0.0, exercises[1].weight, 1e-9)
        assertEquals(7.5, exercises[2].weight, 1e-9)
    }

    @Test
    fun planFieldsSurviveRoundTrip() {
        val original = plan(Exercise("深蹲", 5, "5", 100.0))
        val decoded = plansFromJson(plansToJson(listOf(original)))

        val p = decoded.single()
        assertEquals("p1", p.id)
        assertEquals("胸", p.part)
        assertEquals("胸 日", p.title)
        assertEquals(false, p.custom)
        assertEquals("深蹲", p.exercises.single().name)
        assertEquals(5, p.exercises.single().sets)
        assertEquals("5", p.exercises.single().reps)
    }

    @Test
    fun multiplePlansKeepOwnWeights() {
        val chest = plan(Exercise("卧推", 4, "8", 60.0))
        val back = WorkoutPlan(id = "p2", part = "背", title = "背 日", exercises = listOf(Exercise("划船", 4, "10", 40.0)))
        val decoded = plansFromJson(plansToJson(listOf(chest, back)))

        assertEquals(2, decoded.size)
        assertEquals(60.0, decoded[0].exercises.single().weight, 1e-9)
        assertEquals(40.0, decoded[1].exercises.single().weight, 1e-9)
    }

    @Test
    fun missingWeightFieldFallsBackToBodyweight() {
        // 老版本写进去的数据没有 weight 字段，读出来必须是 0（自重），不能崩。
        val oldJson = """[{"id":"p1","part":"腿","title":"腿 日","exercises":[{"name":"深蹲","sets":4,"reps":"10"}]}]"""
        val decoded = plansFromJson(oldJson)

        assertEquals(1, decoded.size)
        assertEquals(0.0, decoded.single().exercises.single().weight, 1e-9)
    }

    @Test
    fun blankPlanPartIsSkipped() {
        val json = """[{"id":"x","part":"","exercises":[]},{"id":"p1","part":"胸","exercises":[]}]"""
        val decoded = plansFromJson(json)

        assertEquals(1, decoded.size)
        assertEquals("胸", decoded.single().part)
    }

    @Test
    fun emptyJsonArrayDecodesToEmptyList() {
        assertTrue(plansFromJson("[]").isEmpty())
    }
}
