package net.typeblog.screenshot.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.TargetApi
import android.content.res.Resources
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import androidx.appcompat.view.ContextThemeWrapper

import net.typeblog.screenshot.R
import net.typeblog.screenshot.ui.AutoScreenshotButton

import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

// The service to automatically scroll the screen and produce screenshots
// Although the user still needs to manually click on a button each time a new screenshot
// should be taken, this still seems more convenient than having to press Volume Down + Power
// endlessly.
// This only works on P and above and will only be invoked if so (see MainActivity)
@TargetApi(Build.VERSION_CODES.P)
class AutoScreenshotService: AccessibilityService() {
    data class TakeScreenshotEvent(
        val isFirstTime: Boolean
    )
    class ShowButtonEvent

    private val mScreenHeight = Resources.getSystem().displayMetrics.heightPixels
    private val mScreenWidth = Resources.getSystem().displayMetrics.widthPixels

    private val mHandler = Handler(Looper.getMainLooper())

    private lateinit var mButton: AutoScreenshotButton

    override fun onInterrupt() {

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        mButton = AutoScreenshotButton(
            ContextThemeWrapper(this, R.style.AppTheme))
        // This event is sent from MainActivity's floating button to scroll & produce one screenshot
        EventBus.getDefault().register(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        // If we are getting destroyed...
        mButton.hide()
        EventBus.getDefault().unregister(this)
    }

    private fun scrollAndShot() {
        val gestureBuilder = GestureDescription.Builder()
        val path = Path()
        path.moveTo(mScreenWidth.toFloat() / 2, mScreenHeight.toFloat() / 4 * 3)
        path.lineTo(mScreenWidth.toFloat() / 2, mScreenHeight.toFloat() / 6)
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 100, 500))
        dispatchGesture(gestureBuilder.build(), object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                // Wait for some time because the interface might have not updated yet
                mHandler.postDelayed({
                    performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
                }, 100)
            }
        }, null)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused")
    fun onTakeScreenshotEvent(ev: TakeScreenshotEvent) {
        if (ev.isFirstTime) {
            // If it's the first time, don't scroll
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
        } else {
            scrollAndShot()
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    @Suppress("unused", "UNUSED_PARAMETER")
    fun onShowButton(ev: ShowButtonEvent) {
        if (!mButton.isShown) {
            mButton.show()
        }
    }
}