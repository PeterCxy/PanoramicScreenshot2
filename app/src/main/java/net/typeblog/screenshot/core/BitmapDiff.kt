package net.typeblog.screenshot.core

import android.graphics.Bitmap

import java.lang.Exception

class BitmapDiff(bmp1: Bitmap, bmp2: Bitmap,
                 private val mThreshold: Float,
                 private val mWidthIndicies: IntArray,
                 private val mListener: ScreenshotComposer.ProgressListener) {
    class DimensionMismatchException: Exception()

    val mBmps: Pair<Bitmap, Bitmap> = Pair(bmp1, bmp2)

    // We use the "common" part of two bitmaps to represent
    // the "difference" of them
    val diff: CommonSubstring by lazy {
        calculateDiff()
    }

    init {
        if (!checkDimensions()) {
            throw DimensionMismatchException()
        }

        assert(mThreshold <= 1)
    }

    private fun checkDimensions(): Boolean =
        mBmps.first.width == mBmps.second.width &&
                mBmps.first.height == mBmps.second.height

    private fun calculateDiff(): CommonSubstring {
        // This method will only be called once (the first use of `diff`)
        // so it is safe to just notify the caller here about the progress
        // all the later use of `diff` are of no significance and can be ignored
        mListener.onDiffNext()

        val leftLines = mBmps.first.toLines()
        val rightLines = mBmps.second.toLines()

        val commons = lcs(leftLines, rightLines)
        // TODO: how to deal with multiple common substrs?
        return commons[0]
    }

    private fun Bitmap.toLines(): List<BitmapLine> =
        (0 until height).map { i ->
            BitmapLine(this, i, mThreshold, mWidthIndicies)
        }
}