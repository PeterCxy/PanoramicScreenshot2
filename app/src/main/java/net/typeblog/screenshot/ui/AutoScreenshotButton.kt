package net.typeblog.screenshot.ui

import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast

import net.typeblog.screenshot.R
import net.typeblog.screenshot.service.AutoScreenshotService
import net.typeblog.screenshot.service.NotificationDismissService

import org.greenrobot.eventbus.EventBus

import org.jetbrains.anko.UI
import org.jetbrains.anko.*
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onLongClick

class AutoScreenshotButton(private val mContext: Context) {
    private var mAutoScreenshotCount = 0
    private val mView = mContext.UI {
        linearLayout {
            floatingActionButton {
                imageResource = R.drawable.ic_add_a_photo_white_24dp
                onClick {
                    onClick()
                }
                onLongClick {
                    onLongClick()
                }
            }.lparams {
                width = wrapContent
                height = wrapContent
                margin = dip(16)
            }
        }
    }.view

    var isShown = false

    fun show() {
        isShown = true
        mAutoScreenshotCount = 0

        // Tell NotificationDismissService to start dismissing screenshot events
        EventBus.getDefault().post(
            NotificationDismissService.NotificationDismissEvent(true)
        )

        mContext.windowManager.addView(mView, WindowManager.LayoutParams().apply {
            gravity = Gravity.BOTTOM or Gravity.START
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            width = wrapContent
            height = wrapContent
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        })
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun hide() {
        isShown = false

        // Do not continue to dismiss any screenshot notifications...
        EventBus.getDefault().post(
            NotificationDismissService.NotificationDismissEvent(false)
        )

        mContext.windowManager.removeView(mView)
    }

    private fun onClick() {
        EventBus.getDefault().post(
            AutoScreenshotService.TakeScreenshotEvent(
                mAutoScreenshotCount == 0
            )
        )
        // Record the count so we can find the screenshots later
        mAutoScreenshotCount++
    }

    private fun onLongClick() {
        hide()
        // Do not continue to dismiss any screenshot notifications...
        EventBus.getDefault().post(
            NotificationDismissService.NotificationDismissEvent(false)
        )

        val showToast = {
            Toast.makeText(mContext, R.string.screenshot_finished, Toast.LENGTH_LONG).show()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // On Q, we can query for the latest screenshots,
            // and fill in ComposeActivity automatically
            findLatestPictures()?.let {
                mContext.startActivity(
                    Intent(
                        mContext,
                        ComposeActivity::class.java
                    ).apply {
                        putParcelableArrayListExtra(
                            "uris",
                            ArrayList(it.reversed())
                        )
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
            } ?: showToast()
        } else {
            showToast()
        }
    }

    // Find the latest screenshots from gallery
    // which should be the ones the user has just taken
    // TODO: implement this on <= P
    @TargetApi(Build.VERSION_CODES.Q)
    private fun findLatestPictures(): List<Uri>? {
        if (mAutoScreenshotCount == 0) return null

        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DATE_MODIFIED,
            MediaStore.Images.ImageColumns.RELATIVE_PATH
        )
        val cursor = mContext.contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                "${MediaStore.Images.ImageColumns.RELATIVE_PATH} =?",
                arrayOf("Pictures/Screenshots/"),
                "${MediaStore.Images.ImageColumns.DATE_MODIFIED} DESC"
            )

        return if (cursor?.moveToFirst() == true) {
            (0 until mAutoScreenshotCount).map {
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
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