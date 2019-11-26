package net.typeblog.screenshot.ui

import android.annotation.TargetApi
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.PixelFormat
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
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
    private var isEnabled = true
    private lateinit var mProgressBar: View
    private val mView = mContext.UI {
        linearLayout {
            progressFab {
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
                }

                mProgressBar = progressBar {
                    indeterminateDrawable.colorFilter = PorterDuffColorFilter(
                        mContext.getColor(R.color.colorButtonProgress), PorterDuff.Mode.SRC_IN)
                }.lparams {
                    width = wrapContent
                    height = wrapContent
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
        isEnabled = true
        mView.visibility = View.VISIBLE
        mProgressBar.visibility = View.INVISIBLE

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

    // Temporarily hide the button so that it does not end up in screenshots
    fun hideButtonForScreenshot() {
        mView.visibility = View.INVISIBLE
        mView.postDelayed({
            mView.visibility = View.VISIBLE
        }, 500)
    }

    private fun onClick() {
        // Hack: use our own isEnabled state, since the one of the FAB itself
        //   will remove the elevation and make the progress bar look awful
        if (!isEnabled) return

        EventBus.getDefault().post(
            AutoScreenshotService.TakeScreenshotEvent(
                mAutoScreenshotCount == 0
            )
        )
        // Record the count so we can find the screenshots later
        mAutoScreenshotCount++

        // Force a minimal delay between two clicks
        // This is to ensure at least the last screenshot is being saved
        // Also to ensure later findLatestPictures() fetches all of them
        isEnabled = false
        mProgressBar.visibility = View.VISIBLE
        mView.postDelayed({
            isEnabled = true
            mProgressBar.visibility = View.INVISIBLE
        }, 3000)
    }

    private fun onLongClick() {
        // Same as in onClick()
        if (!isEnabled) return
        mView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

        // Delay for some reasonable amount of time so that the shot is saved before continuing...
        // There is really no better way for this... And I think it is reasonable
        mProgressBar.visibility = View.VISIBLE
        isEnabled = false
        mView.postDelayed({ doFinalize() }, 4000)
    }

    private fun doFinalize() {
        hide()

        // Do not continue to dismiss any screenshot notifications...
        EventBus.getDefault().post(
            NotificationDismissService.NotificationDismissEvent(false)
        )

        val showToast = {
            Toast.makeText(mContext, R.string.screenshot_finished, Toast.LENGTH_LONG).show()
        }

        // fill in ComposeActivity automatically
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
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun queryLatestPicturesQ(): Cursor? {
        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DATE_MODIFIED,
            MediaStore.Images.ImageColumns.RELATIVE_PATH
        )

        return mContext.contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                "${MediaStore.Images.ImageColumns.RELATIVE_PATH} =?",
                arrayOf("Pictures/Screenshots/"),
                "${MediaStore.Images.ImageColumns.DATE_MODIFIED} DESC"
            )
    }

    @TargetApi(Build.VERSION_CODES.P)
    private fun queryLatestPicturesP(): Cursor? {
        val projection = arrayOf(
            MediaStore.Images.ImageColumns._ID,
            MediaStore.Images.ImageColumns.DATE_MODIFIED,
            MediaStore.Images.ImageColumns.DATA
        )

        return mContext.contentResolver
            .query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
                "${MediaStore.Images.ImageColumns.DATA} like ?",
                arrayOf("%Pictures/Screenshots%"),
                "${MediaStore.Images.ImageColumns.DATE_MODIFIED} DESC"
            )
    }

    // Find the latest screenshots from gallery
    // which should be the ones the user has just taken
    private fun findLatestPictures(): List<Uri>? {
        if (mAutoScreenshotCount == 0) return null

        val cursor = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            queryLatestPicturesQ()
        } else {
            queryLatestPicturesP()
        }

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