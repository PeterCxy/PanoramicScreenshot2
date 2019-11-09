package net.typeblog.screenshot.ui

import androidx.appcompat.app.AppCompatActivity

import org.jetbrains.anko.*
import org.jetbrains.anko.appcompat.v7.toolbar
import org.jetbrains.anko.design.appBarLayout

const val ID_TOOLBAR = 2333
const val ID_APPBAR = 2334

fun _RelativeLayout.appBar(activity: AppCompatActivity) = appBarLayout {
        id = ID_APPBAR
        activity.setSupportActionBar(toolbar {
            id = ID_TOOLBAR
        })
    }.lparams(width = matchParent, height = wrapContent) {
        alignParentTop()
    }