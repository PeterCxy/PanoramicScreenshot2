package net.typeblog.screenshot.ui

import android.animation.LayoutTransition
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.transition.Transition
import android.view.*
import androidx.appcompat.app.AppCompatActivity

import com.otaliastudios.zoom.ZoomApi
import com.otaliastudios.zoom.ZoomImageView

import net.typeblog.screenshot.R

import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.sdk27.coroutines.onTouch

open class ImageViewActivity: AppCompatActivity() {
    companion object {
        var picBmp: Bitmap? = null
    }

    private lateinit var mGestureDetector: GestureDetector
    lateinit var mZoomView: ZoomImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mGestureDetector = GestureDetector(this, ImageGestureListener())

        val picUri = intent.getParcelableExtra<Uri>("picUri")

        relativeLayout {
            layoutTransition = LayoutTransition()
            // We do not need the shadowed AppBar here
            setSupportActionBar(toolbar {
                backgroundColorResource = R.color.transparent
                elevation = 1.0f
            }.lparams(width = matchParent, height = wrapContent) {
                alignParentTop()
                topMargin = getStatusBarHeight()
            })

            mZoomView = zoomImageView {
                setHorizontalPanEnabled(true)
                setVerticalPanEnabled(true)
                setZoomEnabled(true)
                setFlingEnabled(true)
                setScrollEnabled(true)
                setOverScrollHorizontal(true)
                setOverScrollVertical(true)
                setOverPinchable(true)
                isHorizontalScrollBarEnabled = true
                isVerticalScrollBarEnabled = true
                setMinZoom(0.1f, ZoomApi.TYPE_ZOOM)
                setMaxZoom(2.5f, ZoomApi.TYPE_ZOOM)
                setAnimationDuration(500)
                transitionName = "image"

                if (picUri != null) {
                    setImageDrawable(
                        Drawable.createFromStream(contentResolver.openInputStream(picUri), picUri.toString()))
                } else {
                    setImageDrawable(
                        BitmapDrawable(resources, picBmp)
                    )
                }
            }.lparams {
                width = matchParent
                height = matchParent
            }.apply {
                onTouch { _, event -> mGestureDetector.onTouchEvent(event) }
            }
        }

        supportActionBar!!.apply {
            setDisplayHomeAsUpEnabled(true)
            hide()
            title = ""
        }

        window.apply {
            clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
            addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            statusBarColor = getColor(R.color.transparent)
            navigationBarColor = getColor(R.color.transparent)

            sharedElementEnterTransition.addListener(object: Transition.TransitionListener {
                override fun onTransitionEnd(transition: Transition?) {
                    mZoomView.zoomTo(0.9f, true)
                    supportActionBar!!.show()
                }

                override fun onTransitionResume(transition: Transition?) {
                }

                override fun onTransitionPause(transition: Transition?) {
                }

                override fun onTransitionCancel(transition: Transition?) {
                }

                override fun onTransitionStart(transition: Transition?) {
                }
            })
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            // Simulate back button to allow shared-element animation to run
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        supportActionBar!!.hide()
        mZoomView.zoomTo(1f, true)
        mZoomView.postDelayed({
            super.onBackPressed()
        }, 500 /* Animation Duration */)
    }

    inner class ImageGestureListener: GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            // Double Tap = switch System UI visibility
            window.apply {
                if ((decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN) != 0) {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    supportActionBar!!.show()
                } else {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    supportActionBar!!.hide()
                }
            }
            return true
        }
    }
}