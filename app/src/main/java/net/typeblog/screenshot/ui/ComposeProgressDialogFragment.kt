package net.typeblog.screenshot.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment

import net.typeblog.screenshot.R
import net.typeblog.screenshot.core.BitmapDiff
import net.typeblog.screenshot.core.ScreenshotComposer

import org.jetbrains.anko.*
import org.jetbrains.anko.support.v4.*

class ComposeProgressDialogFragment(
    private val mUris: List<Uri>,
    private val mSensitivity: Float,
    private val mSkip: Float,
    private val mSampleRatio: Int): DialogFragment() {

    companion object {
        const val ID_PROGRESS_DIFF = 122333
        const val ID_PROGRESS_DIFF_TEXT = 122334
    }

    private lateinit var mProgressDiff: ProgressBar
    private lateinit var mProgressDiffText: TextView
    private var mProgressDiffVal = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = UI {
        relativeLayout {
            padding = dip(10)

            mProgressDiff = horizontalProgressBar {
                id = ID_PROGRESS_DIFF
                isIndeterminate = false
            }.lparams {
                width = matchParent
                height = wrapContent
            }

            mProgressDiffText = textView {
                id = ID_PROGRESS_DIFF_TEXT
                textResource = R.string.progress_diff
                gravity = Gravity.CENTER_HORIZONTAL
            }.lparams {
                width = matchParent
                height = wrapContent
                below(ID_PROGRESS_DIFF)
            }

            updateProgressDiff()
        }
    }.view

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // As long as the views are created, it is safe to start composing
        doAsync {
            val listener = object : ScreenshotComposer.ProgressListener {
                override fun onDiffNext() {
                    uiThread {
                        mProgressDiffVal++
                        updateProgressDiff()
                    }
                }

                override fun onComposingStart() {
                    uiThread {
                        mProgressDiff.isIndeterminate = true
                        mProgressDiffText.text = getString(R.string.composing)
                    }
                }
            }

            try {
                val result = ScreenshotComposer(mUris.map {
                    BitmapFactory.decodeStream(context!!.contentResolver.openInputStream(it))
                }, mSensitivity, mSampleRatio, mSkip, listener).compose()

                uiThread {
                    // We can only pass bitmap by static variable
                    // because itw would be too large to fit into a binder call
                    ImageViewActivity.picBmp = result
                    startActivity(Intent(context, ResultActivity::class.java))
                    dismiss()
                }
            } catch (e: BitmapDiff.DimensionMismatchException) {
                uiThread {
                    Toast.makeText(context!!, R.string.dimension_mismatch, Toast.LENGTH_SHORT)
                        .show()
                    dismiss()
                }
            }
        }
    }

    private fun updateProgressDiff() {
        mProgressDiff.max = mUris.size - 1
        mProgressDiff.progress = mProgressDiffVal - 1
        mProgressDiffText.text = getString(R.string.progress_diff, mProgressDiffVal, mProgressDiffVal + 1, mUris.size)
    }
}