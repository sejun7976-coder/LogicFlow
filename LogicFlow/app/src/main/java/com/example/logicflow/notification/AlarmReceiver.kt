package com.example.logicflow.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.logicflow.LogicFlowApp
import com.example.logicflow.data.local.NotificationLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as LogicFlowApp
        val repository = app.container.repository

        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val enabled = repository.isLearningNotificationEnabled()
            val hour = repository.getLearningNotificationHour()
            if (enabled) {
                NotificationHelper.scheduleDailyAlarm(context, hour)
            }
        } else if (intent.action == "com.example.logicflow.ACTION_CHECK_STUDY") {
            val enabled = repository.isLearningNotificationEnabled()
            if (enabled) {
                CoroutineScope(Dispatchers.IO).launch {
                    val hasStudied = repository.hasStudiedToday()
                    if (!hasStudied) {
                        val title = "학습 알림"
                        val content = "오늘 아직 학습을 완료하지 않으셨습니다. 지금 바로 학습을 시작해보세요!"
                        NotificationHelper.showNotification(
                            context = context,
                            title = title,
                            content = content
                        )
                        repository.insertNotificationLog(
                            NotificationLogEntity(
                                id = UUID.randomUUID().toString(),
                                title = title,
                                content = content,
                                timestamp = System.currentTimeMillis()
                            )
                        )
                    }
                }
            }
        }
    }
}
