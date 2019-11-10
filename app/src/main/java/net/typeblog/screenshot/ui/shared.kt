package net.typeblog.screenshot.ui

import android.app.Activity
import android.view.ViewManager
import androidx.appcompat.app.AppCompatActivity
import com.otaliastudios.zoom.ZoomImageView

import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.custom.ankoView
import org.jetbrains.anko.design.appBarLayout

const val ID_TOOLBAR = 2333
const val ID_APPBAR = 2334

fun _RelativeLayout.appBar(activity: AppCompatActivity) = appBarLayout {
        id = ID_APPBAR
        activity.setSupportActionBar(toolbar {
            id = ID_TOOLBAR
        })
    }.lparams(width = matchParent, height = wrapContent) {
        alignParentTop()
    }

inline fun ViewManager.zoomImageView() = zoomImageView { }
inline fun ViewManager.zoomImageView(init: ZoomImageView.() -> Unit) =
    ankoView({ ZoomImageView(it) }, 0, init)

fun Activity.getStatusBarHeight(): Int {
    var result = 0
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId)
    }
    return result
}