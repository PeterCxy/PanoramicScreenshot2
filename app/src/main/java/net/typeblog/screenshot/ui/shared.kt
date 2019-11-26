package net.typeblog.screenshot.ui

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewManager
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

import com.dmitrymalkovich.android.ProgressFloatingActionButton
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

inline fun ViewManager.progressFab(
    init: _ProgressFloatingActionButton.() -> Unit
): ProgressFloatingActionButton =
    ankoView({ _ProgressFloatingActionButton(it) }, 0, init)

@Suppress("ClassName")
class _ProgressFloatingActionButton(context: Context): ProgressFloatingActionButton(context, null) {
    inline fun <T: View> T.lparams(
        init: FrameLayout.LayoutParams.() -> Unit
    ): T {
        val layoutParams = FrameLayout.LayoutParams(0, 0)
        layoutParams.init()
        this@lparams.layoutParams = layoutParams
        return this
    }
}

fun Activity.getStatusBarHeight(): Int {
    var result = 0
    val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId)
    }
    return result
}