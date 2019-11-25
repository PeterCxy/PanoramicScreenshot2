package net.typeblog.screenshot.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.annotation.TargetApi
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.graphics.Path
import android.os.Build
import android.view.accessibility.AccessibilityEvent

// The service to automatically scroll the screen and produce screenshots
// Although the user still needs to manually click on a button each time a new screenshot
// should be taken, this still seems more convenient than having to press Volume Down + Power
// endlessly.
// This only works on P and above and will only be invoked if so (see MainActivity)
@TargetApi(Build.VERSION_CODES.P)
class AutoScreenshotService: AccessibilityService() {
    companion object {
        const val ACTION_SCREENSHOT = "net.typeblog.screenshot.ACTION_SCREENSHOT"
    }

    private val mScreenHeight = Resources.getSystem().displayMetrics.heightPixels
    private val mScreenWidth = Resources.getSystem().displayMetrics.widthPixels

    override fun onInterrupt() {

    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {

    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        // This intent is sent from MainActivity's floating button to scroll & produce one screenshot
        registerReceiver(ScreenshotReceiver(), IntentFilter(ACTION_SCREENSHOT))
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
                performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            }
        }, null)
    }

    inner class ScreenshotReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent!!.getBooleanExtra("first_time", false)) {
                // If it's the first time, don't scroll
                performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            } else {
                scrollAndShot()
            }
        }
    }
}