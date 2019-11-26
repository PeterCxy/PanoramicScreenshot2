package net.typeblog.screenshot.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import net.typeblog.screenshot.R
import net.typeblog.screenshot.service.AutoScreenshotService
import net.typeblog.screenshot.util.isAccessibilityServiceEnabled
import net.typeblog.screenshot.util.isNotificationAccessEnabled
import org.greenrobot.eventbus.EventBus

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.onClick

class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            backgroundColor = theme.color(android.R.attr.colorPrimary)
            gravity = Gravity.CENTER
            textView(R.string.app_name) {
                textSize = sp(14).toFloat()
                typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                gravity = Gravity.CENTER_HORIZONTAL
            }.lparams(width = matchParent, height = wrapContent) {
                leftMargin = dip(40)
                rightMargin = dip(40)
            }

            button(R.string.start) {
                backgroundResource = attr(android.R.attr.selectableItemBackground).resourceId
                textColorResource = attr(R.attr.colorAccent).resourceId
                onClick {
                    // Enter compose activity
                    startActivity(Intent(this@MainActivity, ComposeActivity::class.java))
                }
            }.lparams(width = matchParent, height = wrapContent) {
                topMargin = dip(20)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // The AccessibilityService method to produce screenshots is only possible >= P
                button(R.string.screenshot_helper) {
                    backgroundResource = attr(android.R.attr.selectableItemBackground).resourceId
                    textColorResource = attr(R.attr.colorAccent).resourceId
                    onClick { createFloatingButton() }
                    isEnabled = !activityManager.isLowRamDevice
                }.lparams(width = matchParent, height = wrapContent) {
                    topMargin = dip(5)
                }
            }
        }
    }

    // TODO: Teach users how to use this button (especially to ignore the notification)
    // TODO: Find a way to request these in one batch instead of requiring users to click again and again
    private fun createFloatingButton() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                // Ask for external storage permission
                // This is required for automatic filling after finishing
                requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 0)
                return
            }
        }

        if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this).apply {
                setMessage(R.string.enable_overlay_perms)
                setPositiveButton(R.string.ok) { _, _ ->
                    startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                }
                setNegativeButton(R.string.cancel, null)
            }.show()
            return
        }

        if (!isNotificationAccessEnabled(this)) {
            AlertDialog.Builder(this).apply {
                setMessage(R.string.enable_notification_access)
                setPositiveButton(R.string.ok) { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                setNegativeButton(R.string.cancel, null)
            }.show()
            return
        }

        if (!isAccessibilityServiceEnabled(this, AutoScreenshotService::class.java)) {
            AlertDialog.Builder(this).apply {
                setMessage(R.string.enable_accessibility)
                setPositiveButton(R.string.ok) { _, _ ->
                    startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                }
                setNegativeButton(R.string.cancel, null)
            }.show()
            return
        }

        // Let the service show the button, to avoid having this activity referenced by WM
        EventBus.getDefault().post(AutoScreenshotService.ShowButtonEvent())

        // Stop this activity when we get the button shown
        finish()
    }
}