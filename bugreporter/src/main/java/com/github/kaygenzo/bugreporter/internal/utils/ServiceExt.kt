package com.github.kaygenzo.bugreporter.internal.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.github.kaygenzo.bugreporter.R

internal fun Service.startAsForeground(channelID: String, channelName: String, notificationID: Int) {
    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel =
            NotificationChannel(channelID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(
            Context.NOTIFICATION_SERVICE
        ) as NotificationManager
        notificationManager.createNotificationChannel(channel)
        NotificationCompat.Builder(this, channelID)
    } else {
        NotificationCompat.Builder(this)
    }
    val notification = builder
        .setContentTitle(getString(R.string.bug_reporter_notification_title))
        .setTicker(getString(R.string.bug_reporter_notification_title))
        .setContentText(getString(R.string.bug_reporter_notification_content))
        .setSmallIcon(R.drawable.ic_baseline_bug_report_24)
        .build()
    startForeground(notificationID, notification)
}