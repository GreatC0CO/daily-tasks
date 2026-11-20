package com.deepseek.dailytasks.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.CalendarContract
import java.time.LocalDate
import java.time.ZoneId

/** 把一条任务写进手机自带日历：走系统日历的「新建事件」界面，用户确认即可。 */
object CalendarHelper {

    fun addEvent(context: Context, title: String, date: LocalDate, startMinutes: Int, note: String): Boolean {
        val start = date.atStartOfDay().plusMinutes(startMinutes.toLong())
        val beginMillis = start.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endMillis = start.plusHours(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, title)
            putExtra(CalendarContract.Events.DESCRIPTION, note.ifBlank { "来自「每日清单」" })
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endMillis)
            putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (_: ActivityNotFoundException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }
}
