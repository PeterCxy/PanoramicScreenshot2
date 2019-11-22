package net.typeblog.screenshot.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import net.typeblog.screenshot.R

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class ResultActivity: ImageViewActivity() {
    companion object {
        const val ID_MENU_SAVE = 13333
        val SAVE_PATH = "${Environment.DIRECTORY_PICTURES}/PanoramicScreenshot"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // We don't have transition animation for this, so just show the actionbar
        supportActionBar!!.show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu!!.add(0, ID_MENU_SAVE, ID_MENU_SAVE, R.string.save).apply {
            icon = getDrawable(R.drawable.ic_save_white_24dp)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            ID_MENU_SAVE -> savePicture()
        }
        return false
    }

    private fun savePicture() {
        // Generate a file name based on date
        val dtf = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")
        val name = dtf.format(LocalDateTime.now())

        // Save via MediaStore API
        // This is REQUIRED for Android Q (10)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, SAVE_PATH)
            }
        }

        contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)?.let {
            contentResolver.openOutputStream(it).also { stream ->
                picBmp!!.compress(Bitmap.CompressFormat.PNG, 100, stream)
                Toast.makeText(this@ResultActivity,
                    getString(R.string.saved_to, SAVE_PATH),
                    Toast.LENGTH_LONG).show()
            }
        }
    }
}