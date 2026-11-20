package com.deepseek.dailytasks.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.deepseek.dailytasks.data.TaskStore

/** 每天 00:05 把"今天"的任务提醒重新排一遍。 */
class DailyRolloverReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderScheduler.ACTION_ROLLOVER) return
        val store = TaskStore(context)
        store.pruneHistory()
        ReminderScheduler.rescheduleAll(context, store.loadTasks())
    }
}

/** 开机、时间被改、时区变化、应用更新后重建闹钟。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> {
                val store = TaskStore(context)
                ReminderScheduler.rescheduleAll(context, store.loadTasks())
            }
        }
    }
}
