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

actual fun showSessionIdleNotification(sessionTitle: String, sessionId: String, directory: String) {
    val context = AppContextHolder.applicationContext ?: return
    val channelId = "opencode_agent"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val channel = NotificationChannel(
            channelId,
            "Agent",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when the agent finishes"
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        putExtra("opencode_session_id", sessionId)
        putExtra("opencode_directory", directory)
    } ?: return
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val title = sessionTitle.ifBlank { "OpenCode" }
    val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        android.app.Notification.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Agent finished")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    } else {
        @Suppress("DEPRECATION")
        android.app.Notification.Builder(context)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText("Agent finished")
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    try {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(sessionTitle.hashCode(), notification)
    } catch (e: SecurityException) {
        // Notification permission not granted
    }
}
