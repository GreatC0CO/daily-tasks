package com.deepseek.dailytasks.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.deepseek.dailytasks.data.TaskStore

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_REMIND) return
        val taskId = intent.getLongExtra(ReminderScheduler.EXTRA_TASK_ID, 0L)
        Log.d("ReminderReceiver", "reminder fired for task $taskId")

        val store = TaskStore(context)
        val tasks = store.loadTasks()
        val task = tasks.firstOrNull { it.id == taskId }
        if (task != null && task.title.isNotBlank()) {
            NotificationHelper.showReminder(
                context = context,
                id = (taskId % Int.MAX_VALUE).toInt(),
                title = task.title,
                timeText = task.timeText,
                note = task.note,
            )
        }
        // 触发后立刻排下一次，保证"每 N 天"这种周期能自己滚下去
        ReminderScheduler.rescheduleAll(context, tasks)
    }
}
