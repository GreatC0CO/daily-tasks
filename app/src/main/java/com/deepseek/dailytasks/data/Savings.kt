package com.deepseek.dailytasks.data

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/** 攒钱任务的类型：里程碑型（一共做满 N 次就收工）和日常积累型（每天都能做，可设每天上限）。 */
enum class SavingTaskKind(val label: String, val hint: String) {
    MILESTONE("里程碑任务", "设一个总次数，比如「跑 30 次」，每完成一次攒一次，做满就结束"),
    DAILY("日常积累任务", "每天都能完成，可以设每天最多几次（比如每天最多 3 次）"),
}

/** 目标下面挂的攒钱任务：完成了就攒 [amount] 元。 */
data class SavingTask(
    val id: Long,
    val name: String,
    val amount: Double,
    val kind: SavingTaskKind = SavingTaskKind.DAILY,
    /** 里程碑任务：总共要完成几次（0 = 不限次数，一直可以做）。 */
    val targetCount: Int = 0,
    /** 日常积累任务：每天最多能领几次；0 = 不限。 */
    val maxPerDay: Int = 0,
) {
    val amountText: String get() = moneyText(amount)

    val isMilestone: Boolean get() = kind == SavingTaskKind.MILESTONE

    val isDaily: Boolean get() = kind == SavingTaskKind.DAILY

    /** 里程碑任务是不是设了总次数。 */
    val hasTarget: Boolean get() = isMilestone && targetCount > 0

    /** 日常任务是不是设了每天上限。 */
    val limited: Boolean get() = isDaily && maxPerDay > 0

    /** 任务规则的一句话说明，比如「里程碑 · 共 30 次」「每天最多 3 次」。 */
    val ruleText: String
        get() = when {
            hasTarget -> "里程碑 · 共 $targetCount 次"
            isMilestone -> "里程碑 · 不限次数"
            limited -> "日常 · 每天最多 $maxPerDay 次"
            else -> "日常 · 不限次数"
        }
}

/** 一条攒钱记录（哪一天、哪个任务、攒了多少）。 */
data class SavingContribution(
    val epochDay: Long,
    val taskId: Long,
    val taskName: String,
    val amount: Double,
) {
    val date: LocalDate get() = LocalDate.ofEpochDay(epochDay)
    val dateText: String get() = date.format(Savings.FMT)
    val amountText: String get() = moneyText(amount)
}

/**
 * 攒钱目标：想买的东西 + 金额，下面挂若干攒钱任务。
 * [savedAmount] 是已经攒到的钱，等于所有 [contributions] 金额之和。
 */
data class SavingGoal(
    val id: Long,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double = 0.0,
    val note: String = "",
    val tasks: List<SavingTask> = emptyList(),
    val contributions: List<SavingContribution> = emptyList(),
) {
    /** 0f..1f 的完成度，存钱罐按这个比例灌。 */
    val progress: Float
        get() = if (targetAmount <= 0.0) 0f else (savedAmount / targetAmount).toFloat().coerceIn(0f, 1f)

    val percentText: String get() = String.format(Locale.US, "%.0f%%", progress * 100)

    val remaining: Double get() = (targetAmount - savedAmount).coerceAtLeast(0.0)

    val isComplete: Boolean get() = targetAmount > 0.0 && savedAmount >= targetAmount

    val targetText: String get() = moneyText(targetAmount)
    val savedText: String get() = moneyText(savedAmount)
    val remainingText: String get() = moneyText(remaining)

    /** 最近攒到的日期（没有记录就是 null）。 */
    val lastSavedDate: LocalDate? get() = contributions.maxOfOrNull { it.epochDay }?.let(LocalDate::ofEpochDay)
}

/** 本月必要开支里的一项：房租、水电、饭钱之类。 */
data class BudgetItem(
    val id: Long,
    val name: String,
    val amount: Double,
    val note: String = "",
    /** 已经为哪个月预留好（"2026-11"）；换月后自动变成「未预留」。 */
    val reservedMonth: String = "",
    /** 每 [decreaseEveryDays] 天减少多少（>0 才算「会降低」的开支）；0 表示固定金额。 */
    val decreaseAmount: Double = 0.0,
    /** 多少天降低一次：1 = 每天，3 = 每 3 天。 */
    val decreaseEveryDays: Int = 1,
    /** 降低的起算日（epochDay）；0 表示从本月 1 号起算。 */
    val decreaseStartEpochDay: Long = 0L,
) {
    /** 原始金额（编辑时看到的本月总额）。 */
    val amountText: String get() = moneyText(amount)

    fun isReserved(month: String): Boolean = reservedMonth == month

    /** 是不是会随时间慢慢变少的开支。 */
    val isDecreasing: Boolean get() = decreaseAmount > 0.0 && decreaseEveryDays >= 1

    /** 降低的起算日期：默认本月 1 号。 */
    fun decreaseStart(today: LocalDate): LocalDate =
        if (decreaseStartEpochDay > 0L) LocalDate.ofEpochDay(decreaseStartEpochDay) else today.withDayOfMonth(1)

    /** 从起算日到今天，已经过了几个「降低周期」。 */
    fun passedSteps(today: LocalDate): Int {
        if (!isDecreasing) return 0
        val days = today.toEpochDay() - decreaseStart(today).toEpochDay()
        if (days <= 0L) return 0
        return (days / decreaseEveryDays).toInt()
    }

    /** 今天还剩多少（固定开支就是原金额；递减开支按已过的天数往下扣，最低到 0）。 */
    fun remainingOn(today: LocalDate): Double {
        val base = amount.coerceAtLeast(0.0)
        if (!isDecreasing) return base
        return (base - passedSteps(today) * decreaseAmount).coerceAtLeast(0.0)
    }

    fun remainingText(today: LocalDate): String = moneyText(remainingOn(today))

    /** 「每天 −¥100」「每 3 天 −¥50」这样的说明（固定开支是空串）。 */
    val decreaseText: String
        get() = if (!isDecreasing) {
            ""
        } else {
            val every = if (decreaseEveryDays == 1) "每天" else "每 ${decreaseEveryDays} 天"
            "$every −${moneyText(decreaseAmount)}"
        }

    /** 下一次降低是哪天（固定开支是 null）。 */
    fun nextDecreaseDate(today: LocalDate): LocalDate? {
        if (!isDecreasing) return null
        val nextStep = passedSteps(today) + 1
        return decreaseStart(today).plusDays(nextStep.toLong() * decreaseEveryDays)
    }

    /** 已经扣到 0 了（递减开支才有意义）。 */
    fun isExhausted(today: LocalDate): Boolean = isDecreasing && remainingOn(today) <= 0.0
}

/** 本月必要开支的汇总数字。 */
data class BudgetSummary(
    val total: Double,
    val reserved: Double,
    val remaining: Double,
    val count: Int,
    val reservedCount: Int,
) {
    val totalText: String get() = moneyText(total)
    val reservedText: String get() = moneyText(reserved)
    val remainingText: String get() = moneyText(remaining)
    val progress: Float get() = if (total <= 0.0) 0f else (reserved / total).toFloat().coerceIn(0f, 1f)
}

/** 攒钱相关的纯逻辑（方便单元测试）。 */
object Savings {

    val FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/M/d", Locale.CHINA)

    fun monthKey(date: LocalDate): String = String.format(Locale.US, "%04d-%02d", date.year, date.monthValue)

    fun monthLabel(date: LocalDate): String =
        String.format(Locale.US, "%04d 年 %d 月", date.year, date.monthValue)

    /** 这个任务今天已经领了几次。 */
    fun claimedToday(goal: SavingGoal, task: SavingTask, today: LocalDate): Int =
        goal.contributions.count { it.taskId == task.id && it.epochDay == today.toEpochDay() }

    /** 这个任务总共已经领了几次（里程碑任务用。 */
    fun claimedTotal(goal: SavingGoal, task: SavingTask): Int =
        goal.contributions.count { it.taskId == task.id }

    /** 这个任务今天还能不能领（里程碑看总次数，日常看每天上限）。 */
    fun canClaim(goal: SavingGoal, task: SavingTask, today: LocalDate): Boolean = when {
        task.isMilestone -> !task.hasTarget || claimedTotal(goal, task) < task.targetCount
        else -> !task.limited || claimedToday(goal, task, today) < task.maxPerDay
    }

    /** 里程碑任务是否已经做满（日常任务恒为 false）。 */
    fun isFinished(goal: SavingGoal, task: SavingTask): Boolean =
        task.hasTarget && claimedTotal(goal, task) >= task.targetCount

    /** 里程碑任务的完成度 0f..1f；日常任务返回 -1f（没有总进度）。 */
    fun taskProgress(goal: SavingGoal, task: SavingTask): Float {
        if (!task.hasTarget) return -1f
        return (claimedTotal(goal, task).toFloat() / task.targetCount).coerceIn(0f, 1f)
    }

    /**
     * 今天还剩几次可领；不限次数的返回 -1。
     * 里程碑任务返回的是「总共还差几次」，和今天无关。
     */
    fun remainingClaims(goal: SavingGoal, task: SavingTask, today: LocalDate): Int = when {
        task.isMilestone -> if (task.hasTarget) {
            (task.targetCount - claimedTotal(goal, task)).coerceAtLeast(0)
        } else {
            -1
        }
        !task.limited -> -1
        else -> (task.maxPerDay - claimedToday(goal, task, today)).coerceAtLeast(0)
    }

    /** 完成一次任务：记一条记录并把金额加到 [SavingGoal.savedAmount]。 */
    fun claim(goal: SavingGoal, task: SavingTask, today: LocalDate): SavingGoal {
        if (!canClaim(goal, task, today)) return goal
        val contribution = SavingContribution(today.toEpochDay(), task.id, task.name, task.amount)
        return goal.copy(
            savedAmount = goal.savedAmount + task.amount,
            contributions = goal.contributions + contribution,
        )
    }

    /** 撤销某条记录（点错时用），金额一起退回。 */
    fun undo(goal: SavingGoal, contribution: SavingContribution): SavingGoal = goal.copy(
        savedAmount = (goal.savedAmount - contribution.amount).coerceAtLeast(0.0),
        contributions = goal.contributions - contribution,
    )

    /** 攒钱记录按时间倒序（同一天按任务名）。 */
    fun sortedContributions(goal: SavingGoal): List<SavingContribution> =
        goal.contributions.sortedWith(compareByDescending<SavingContribution> { it.epochDay }.thenBy { it.taskName })

    /** 今天还能领的金额合计（限次任务按剩余次数算；里程碑任务按总共还差几次算）。 */
    fun claimableToday(goal: SavingGoal, today: LocalDate): Double =
        goal.tasks.sumOf { task ->
            val left = remainingClaims(goal, task, today)
            when {
                left < 0 -> if (isFinished(goal, task)) 0.0 else task.amount
                else -> task.amount * left
            }
        }

    /** 本月必要开支汇总；递减的开支按「今天还剩多少」计算，预留状态仍按月份判断。 */
    fun budgetSummary(items: List<BudgetItem>, today: LocalDate): BudgetSummary {
        val month = monthKey(today)
        val total = items.sumOf { it.remainingOn(today) }
        val reserved = items.filter { it.isReserved(month) }.sumOf { it.remainingOn(today) }
        return BudgetSummary(
            total = total,
            reserved = reserved,
            remaining = (total - reserved).coerceAtLeast(0.0),
            count = items.size,
            reservedCount = items.count { it.isReserved(month) },
        )
    }

    /** 本月还剩几天（含今天）。 */
    fun daysLeftInMonth(today: LocalDate): Int {
        val length = YearMonth.from(today).lengthOfMonth()
        return (length - today.dayOfMonth + 1).coerceAtLeast(1)
    }

    /** 每天平均还要预留多少（把剩余金额摊到剩余天数）。 */
    fun dailyReserve(summary: BudgetSummary, today: LocalDate): Double {
        val days = daysLeftInMonth(today)
        return if (days <= 0) summary.remaining else summary.remaining / days
    }

    /** 攒钱目标按完成度排序：没攒完的在前，完成度高的在前。 */
    fun sortedGoals(goals: List<SavingGoal>): List<SavingGoal> =
        goals.sortedWith(
            compareBy<SavingGoal> { it.isComplete }
                .thenByDescending { it.progress }
                .thenBy { it.name },
        )
}

/** 金额文案：¥1,234.5 → ¥1234.5（去掉多余的 0）。 */
fun moneyText(value: Double): String {
    val rounded = (value * 100).toLong() / 100.0
    val text = if (rounded == rounded.toLong().toDouble()) {
        rounded.toLong().toString()
    } else {
        String.format(Locale.US, "%.2f", rounded).trimEnd('0').trimEnd('.')
    }
    return "¥$text"
}
