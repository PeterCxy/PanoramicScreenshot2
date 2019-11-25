package net.typeblog.screenshot.ui

import android.Manifest
import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import net.typeblog.screenshot.R
import net.typeblog.screenshot.service.AutoScreenshotService
import net.typeblog.screenshot.service.NotificationDismissService
import net.typeblog.screenshot.util.isAccessibilityServiceEnabled
import net.typeblog.screenshot.util.isNotificationAccessEnabled

import org.jetbrains.anko.*
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onLongClick

class MainActivity: AppCompatActivity() {
    private var mAutoScreenshotCount = 0

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

        mAutoScreenshotCount = 0
        val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val view = UI {
            linearLayout {
                floatingActionButton {
                    imageResource = R.drawable.ic_add_a_photo_white_24dp
                    onClick {
                        sendBroadcast(Intent(AutoScreenshotService.ACTION_SCREENSHOT).apply {
                            putExtra("first_time", mAutoScreenshotCount == 0)
                        })
                        // Record the count so we can find the screenshots later
                        mAutoScreenshotCount++
                    }
                    onLongClick {
                        wm.removeView(view)
                        // Do not continue to dismiss any screenshot notifications...
                        sendBroadcast(Intent(NotificationDismissService.ACTION_STOP_DISMISSING_NOTIFICATIONS))

                        val showToast = {
                            Toast.makeText(context, R.string.screenshot_finished, Toast.LENGTH_LONG).show()
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            // On Q, we can query for the latest screenshots,
                            // and fill in ComposeActivity automatically
                            findLatestPictures()?.let {
                                startActivity(
                                    Intent(
                                        this@MainActivity,
                                        ComposeActivity::class.java
                                    ).apply {
                                        putParcelableArrayListExtra(
                                            "uris",
                                            ArrayList(it.reversed())
                                        )
                                    })
                            } ?: showToast()
                        } else {
                            showToast()
                        }
                    }
                }.lparams {
                    width = wrapContent
                    height = wrapContent
                    margin = dip(16)
                }
            }
        }.view

        wm.addView(view, WindowManager.LayoutParams().apply {
            gravity = Gravity.BOTTOM or Gravity.START
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = wrapContent
            height = wrapContent
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        })
    }

    // Find the latest screenshots from gallery
    // which should be the ones the user has just taken
    @TargetApi(Build.VERSION_CODES.Q)
    private fun findLatestPictures(): List<Uri>? {
        if (mAutoScreenshotCount == 0) return null

        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DATE_MODIFIED,
            MediaStore.Images.ImageColumns.RELATIVE_PATH
        )
        val cursor = contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                "${MediaStore.Images.ImageColumns.RELATIVE_PATH} =?",
                arrayOf("Pictures/Screenshots/"),
                "${MediaStore.Images.ImageColumns.DATE_MODIFIED} DESC"
            )

        return if (cursor?.moveToFirst() == true) {
            (0 until mAutoScreenshotCount).map {
                val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    cursor.getLong(cursor.getColumnIndex(MediaStore.Images.ImageColumns._ID)))
                cursor.moveToNext()
                uri
            }.also { cursor.close() }.toList()
        } else {
            cursor?.close()
            null
        }
    }
}