package com.deepseek.dailytasks.notify

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.deepseek.dailytasks.data.Task
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 用 AlarmManager 安排提醒：
 *  - 每个任务只在"它该出现的那一天"提醒（一次性 / 每天 / 每 N 天都支持）
 *  - 每次提醒触发后都会重新排下一次，所以"每 3 天"这种也会一直滚动下去
 *  - 每天 00:05 再兜底重排一次（跨天刷新、清理过期记录）
 */
object ReminderScheduler {

    const val ACTION_REMIND = "com.deepseek.dailytasks.action.REMIND"
    const val ACTION_ROLLOVER = "com.deepseek.dailytasks.action.ROLLOVER"

    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_TITLE = "extra_title"
    const val EXTRA_TIME = "extra_time"
    const val EXTRA_NOTE = "extra_note"

    private const val REQUEST_BASE = 10_000
    private const val REQUEST_ROLLOVER = 99_001

    fun rescheduleAll(context: Context, tasks: List<Task>) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = Instant.now()
        tasks.forEach { task ->
            cancelAlarm(context, am, task.id)
            if (!task.remind || task.title.isBlank()) return@forEach
            val at = nextTrigger(task, now) ?: return@forEach
            setAlarm(context, am, task.id, at.toEpochMilli())
        }
        scheduleDailyRollover(context, am)
    }

    fun cancel(context: Context, task: Task) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        cancelAlarm(context, am, task.id)
    }

    /** 从当前时刻往后找第一次该提醒的时间点。 */
    private fun nextTrigger(task: Task, now: Instant): Instant? {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        var date = today
        for (i in 0 until 120) {
            if (task.isVisibleOn(date)) {
                val at = date
                    .atTime(task.startMinutes / 60, task.startMinutes % 60)
                    .atZone(zone)
                    .toInstant()
                if (at.isAfter(now)) return at
            }
            date = date.plusDays(1)
        }
        return null
    }

    private fun setAlarm(context: Context, am: AlarmManager, taskId: Long, triggerAtMillis: Long) {
        val pi = reminderPendingIntent(context, taskId)
        val canBeExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        try {
            if (canBeExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: SecurityException) {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
        }
    }

    private fun cancelAlarm(context: Context, am: AlarmManager, taskId: Long) {
        am.cancel(reminderPendingIntent(context, taskId))
    }

    private fun scheduleDailyRollover(context: Context, am: AlarmManager) {
        val intent = Intent(context, DailyRolloverReceiver::class.java).setAction(ACTION_ROLLOVER)
        val pi = PendingIntent.getBroadcast(
            context,
            REQUEST_ROLLOVER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        am.cancel(pi)
        val trigger = LocalDate.now()
            .plusDays(1)
            .atTime(0, 5)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        am.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger,
            AlarmManager.INTERVAL_DAY,
            pi,
        )
    }

    private fun reminderPendingIntent(context: Context, taskId: Long): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ACTION_REMIND
            putExtra(EXTRA_TASK_ID, taskId)
        }
        return PendingIntent.getBroadcast(
            context,
            REQUEST_BASE + (taskId % 100_000).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
