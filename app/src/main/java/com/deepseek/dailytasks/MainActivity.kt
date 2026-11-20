package com.deepseek.dailytasks

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.deepseek.dailytasks.data.TaskStore
import com.deepseek.dailytasks.notify.NotificationHelper
import com.deepseek.dailytasks.notify.ReminderScheduler
import com.deepseek.dailytasks.ui.AppRoot
import com.deepseek.dailytasks.ui.TodayScreen
import com.deepseek.dailytasks.ui.theme.DailyTasksTheme

class MainActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        NotificationHelper.ensureChannel(this)
        askNotificationPermission()

        val store = TaskStore(this)
        store.pruneHistory()
        ReminderScheduler.rescheduleAll(this, store.loadTasks())

        setContent {
            DailyTasksTheme {
                AppRoot(store)
            }
        }
    }

    private fun askNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
