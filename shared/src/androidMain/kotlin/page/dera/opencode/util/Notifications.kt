package page.dera.opencode.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

object AppContextHolder {
    @Volatile
    var applicationContext: Context? = null
}

private var channelCreated = false

actual fun showBackgroundNotification(title: String, body: String, sessionId: String, directory: String) {
    val context = AppContextHolder.applicationContext ?: return
    val channelId = "opencode_agent_alerts"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !channelCreated) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.deleteNotificationChannel(channelId)
        val channel = NotificationChannel(
            channelId,
            "Agent",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications when the agent finishes"
            enableLights(true)
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(channel)
        channelCreated = true
    }

    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("opencode_session_id", sessionId)
        putExtra("opencode_directory", directory)
    } ?: return
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val title = title.ifBlank { "OpenCode" }
    val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        android.app.Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(android.app.Notification.DEFAULT_ALL)
            .build()
    } else {
        @Suppress("DEPRECATION")
        android.app.Notification.Builder(context)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setDefaults(android.app.Notification.DEFAULT_ALL)
            .setPriority(android.app.Notification.PRIORITY_HIGH)
            .build()
    }

    try {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(sessionId.hashCode(), notification)
    } catch (e: SecurityException) {
        // Notification permission not granted
    }
}
