package net.typeblog.screenshot.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

// Dismisses all screenshot notifications during automatic screenshot sessions
class NotificationDismissService: NotificationListenerService() {
    companion object {
        const val ACTION_STOP_DISMISSING_NOTIFICATIONS = "net.typeblog.screenshot.ACTION_STOP_DISMISSING_NOTIFICATIONS"
    }

    private var mShouldDismiss = false

    override fun onListenerConnected() {
        super.onListenerConnected()
        registerReceiver(ScreenshotReceiver(), IntentFilter(AutoScreenshotService.ACTION_SCREENSHOT).apply{
            addAction(ACTION_STOP_DISMISSING_NOTIFICATIONS)
        })
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || !mShouldDismiss) return

        // I'm too lazy to try to actually detect if it's screenshot notification
        // detecting "systemui" should be "good enough"
        if (sbn.key.contains("com.android.systemui")) {
            cancelNotification(sbn.key)
        }
    }

    inner class ScreenshotReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent == null) return
            when (intent.action) {
                AutoScreenshotService.ACTION_SCREENSHOT -> {
                    mShouldDismiss = true
                }
                ACTION_STOP_DISMISSING_NOTIFICATIONS -> {
                    mShouldDismiss = false
                }
            }
        }
    }
}