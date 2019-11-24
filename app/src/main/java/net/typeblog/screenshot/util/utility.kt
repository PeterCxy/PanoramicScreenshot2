package net.typeblog.screenshot.util

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import android.text.TextUtils.SimpleStringSplitter


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

/**
 * Based on [com.android.settingslib.accessibility.AccessibilityUtils.getEnabledServicesFromSettings]
 * @see [AccessibilityUtils](https://github.com/android/platform_frameworks_base/blob/d48e0d44f6676de6fd54fd8a017332edd6a9f096/packages/SettingsLib/src/com/android/settingslib/accessibility/AccessibilityUtils.java.L55)
 */
fun isAccessibilityServiceEnabled(
    context: Context,
    accessibilityService: Class<*>?
): Boolean {
    val expectedComponentName = ComponentName(context, accessibilityService!!)
    val enabledServicesSetting: String = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    )
        ?: return false
    val colonSplitter = SimpleStringSplitter(':')
    colonSplitter.setString(enabledServicesSetting)
    while (colonSplitter.hasNext()) {
        val componentNameString = colonSplitter.next()
        val enabledService = ComponentName.unflattenFromString(componentNameString)
        if (enabledService != null && enabledService == expectedComponentName) return true
    }
    return false
}

fun Context.getStatusBarHeight(): Int {
    var result = 0
    val resourceId: Int = resources.getIdentifier("status_bar_height", "dimen", "android")
    if (resourceId > 0) {
        result = resources.getDimensionPixelSize(resourceId)
    }
    return result
}