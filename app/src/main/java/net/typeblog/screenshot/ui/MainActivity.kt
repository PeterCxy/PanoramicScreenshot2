package net.typeblog.screenshot.ui

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity

import net.typeblog.screenshot.R

import org.jetbrains.anko.*
import org.jetbrains.anko.sdk27.coroutines.*

class MainActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        verticalLayout {
            backgroundColor = theme.color(android.R.attr.colorPrimary)
            gravity = Gravity.CENTER
            textView(R.string.app_name) {
                textSize = sp(14).toFloat()
                typeface = Typeface.create("sans-serif-condensed", Typeface.NORMAL)
                gravity = Gravity.CENTER_HORIZONTAL
            }.lparams(width = matchParent, height = wrapContent) {
                leftMargin = dip(40)
                rightMargin = dip(40)
            }

            button(R.string.start) {
                backgroundResource = attr(android.R.attr.selectableItemBackground).resourceId
                textColorResource = attr(R.attr.colorAccent).resourceId
                onClick {
                    // Enter compose activity
                    startActivity(Intent(this@MainActivity, ComposeActivity::class.java))
                }
            }.lparams(width = matchParent, height = wrapContent) {
                topMargin = dip(20)
            }
        }
    }
}