package net.typeblog.screenshot.core

class BitmapLine(val width: Int, private val threshold: Float,
                 private val widthIndicies: IntArray) {
    val pixels = IntArray(width)

    override fun equals(other: Any?): Boolean {
        if (other !is BitmapLine) {
            return false
        }

        if (other.width != this.width) {
            throw BitmapDiff.DimensionMismatchException()
        }

        // OPTIMIZATION: Use a pre-defined IntArray for indicies
        //    This way, we are actually calling IntArray.count()
        //    instead of Iterator.count(), which is manually inlined
        //    to be a simple for loop. The Iterator implementation
        //    spends all its time in next(), which somehow did not
        //    get inlined by the virtual machine even if it is executed
        //    thousands of times
        val counter = widthIndicies
            .count { i -> this.pixels[i] == other.pixels[i] }

        return counter >= width * threshold
    }
}