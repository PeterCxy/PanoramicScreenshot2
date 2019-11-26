package net.typeblog.screenshot.service

import android.content.ComponentName
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

    init {
        // Start listening here right away
        // because listeners in this class affect the state
        // but does not call any method actively
        // We have to ensure that the state always gets through
        EventBus.getDefault().register(this)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Plz save me...
        requestRebind(ComponentName(this, NotificationDismissService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()

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