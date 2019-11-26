package net.typeblog.screenshot.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

// Dismisses all screenshot notifications during automatic screenshot sessions
class NotificationDismissService: NotificationListenerService() {
    data class NotificationDismissEvent(
        val shouldDismiss: Boolean
    )

    private var mShouldDismiss = false

    override fun onListenerConnected() {
        super.onListenerConnected()
        EventBus.getDefault().register(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        EventBus.getDefault().unregister(this)
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

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onNotificationDismissEvent(ev: NotificationDismissEvent) {
        mShouldDismiss = ev.shouldDismiss
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused", "UNUSED_PARAMETER")
    fun onScreenshotEvent(ev: AutoScreenshotService.TakeScreenshotEvent) {
        // Also set shouldDismiss to true each time a screenshot is taken
        mShouldDismiss = true
    }
}