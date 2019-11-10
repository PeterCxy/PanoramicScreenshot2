package net.typeblog.screenshot.util

import android.app.Activity
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.BitmapFactory
import android.graphics.Bitmap

fun Activity.displayName(uri: Uri): String? {
    val cursor: Cursor? = contentResolver.query(
        uri, null, null, null, null, null)
    cursor?.use {
        // moveToFirst() returns false if the cursor has 0 rows.  Very handy for
        // "if there's anything to look at, look at it" conditionals.
        if (it.moveToFirst()) {
            return it.getString(it.getColumnIndex(OpenableColumns.DISPLAY_NAME))
        }
    }

    return null
}

fun Activity.getPreview(uri: Uri): Bitmap? {
    var stream = contentResolver.openInputStream(uri)
    val bounds = BitmapFactory.Options()
    bounds.inJustDecodeBounds = true
    BitmapFactory.decodeStream(stream, null, bounds)
    if (bounds.outWidth == -1 || bounds.outHeight == -1)
        return null
    stream!!.close()
    stream = contentResolver.openInputStream(uri)

    val originalSize = if (bounds.outHeight > bounds.outWidth)
        bounds.outHeight
    else
        bounds.outWidth

    val opts = BitmapFactory.Options()
    opts.inSampleSize = originalSize / 512 // Sample size: 512px
    return BitmapFactory.decodeStream(stream, null, opts)
}