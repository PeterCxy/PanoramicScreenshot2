package net.typeblog.screenshot.core

class BitmapLine(val width: Int, private val threshold: Float) {
    val pixels = IntArray(width)

    override fun equals(other: Any?): Boolean {
        if (other !is BitmapLine) {
            return false
        }

        if (other.width != this.width) {
            throw BitmapDiff.DimensionMismatchException()
        }

        var counter = 0
        for (i in 0 until width) {
            if (this.pixels[i] == other.pixels[i]) {
                counter++
            }
        }

        return counter >= width * threshold
    }
}