package net.typeblog.screenshot.core

import android.graphics.*

class ScreenshotComposer(bmps: List<Bitmap>, threshold: Float,
                         private val mListener: ProgressListener) {
    interface ProgressListener {
        fun onDiffNext()
        fun onComposingStart()
    }

    private val mWidth = bmps[0].width // All bitmaps must match in width
    private val mWidthIndicies = IntArray(mWidth) { it } // Pre-calculated index array, for BitmapLine
    private val mDiffs = (0 until (bmps.size - 1)).map { i ->
        BitmapDiff(bmps[i], bmps[i + 1], threshold, mWidthIndicies, mListener)
    }

    // Calculate the total height of the final image
    // i.e. the sum of the height of surviving part
    //    of each image + the header of first image
    //    + the footer of the last image
    private fun totalHeight(): Int =
        (0 until (mDiffs.size - 1)).map { i -> accumulativeHeight(i) }
            .sum() + mDiffs[0].diff.leftEnd +
                mDiffs.last().mBmps.second.height - mDiffs.last().diff.rightEnd

    private fun accumulativeHeight(i: Int): Int =
        mDiffs[i + 1].diff.leftEnd - mDiffs[i].diff.rightEnd

    fun compose(): Bitmap {
        val ret = Bitmap.createBitmap(mWidth, totalHeight(), mDiffs[0].mBmps.first.config)

        // Create a Canvas backed by the resulting Bitmap
        Canvas(ret).apply {
            // All the calculateDiff()s should have been executed at this point
            mListener.onComposingStart()

            // Draw the header first bitmap
            drawBitmap(
                mDiffs[0].mBmps.first,
                Rect(0, 0, mWidth, mDiffs[0].diff.leftEnd),
                Rect(0, 0, mWidth, mDiffs[0].diff.leftEnd),
                null
            )

            // Draw all the bitmaps in middle
            val finalY = (0 until (mDiffs.size - 1)).fold(mDiffs[0].diff.leftEnd, { y, i ->
                val srcTop = mDiffs[i].diff.rightEnd
                val srcBottom = srcTop + accumulativeHeight(i)
                val dstBottom = y + accumulativeHeight(i)
                drawBitmap(
                    mDiffs[i].mBmps.second,
                    Rect(0, srcTop, mWidth, srcBottom),
                    Rect(0, y, mWidth, dstBottom),
                    null
                )

                dstBottom
            })

            // Draw the footer of the final bitmap
            drawBitmap(
                mDiffs.last().mBmps.second,
                Rect(0, mDiffs.last().diff.rightEnd, mWidth, mDiffs.last().mBmps.second.height),
                Rect(0, finalY, mWidth, ret.height),
                null
            )
        }

        return ret
    }

    // TODO: add progress listener
}