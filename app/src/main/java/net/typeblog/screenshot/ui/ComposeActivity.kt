package net.typeblog.screenshot.ui

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import net.typeblog.screenshot.util.*

import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.*

class ComposeActivity: AppCompatActivity() {
    companion object {
        const val REQUEST_CHOOSE_PICTURE = 1000
        const val ID_THUMBNAIL = 102333
    }

    private val mUris = ArrayList<Uri>()
    private val mDisplayNames = HashMap<Uri, String?>()
    private val mPreview = HashMap<Uri, Bitmap?>()
    private val mAdapter = SelectionAdapter()

    private var mProgressBarFrame: FrameLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        relativeLayout {
            appBar(this@ComposeActivity)
            recyclerView {
                topPadding = dip(10)
            }.lparams {
                width = matchParent
                height = matchParent
                below(ID_APPBAR)
            }.apply {
                adapter = mAdapter
                layoutManager = LinearLayoutManager(this@ComposeActivity)
            }

            mProgressBarFrame = frameLayout {
                backgroundResource = android.R.color.white
                visibility = View.GONE
                progressBar().lparams {
                    gravity = Gravity.CENTER
                }
            }.lparams {
                width = matchParent
                height = matchParent
                below(ID_APPBAR)
            }
        }

        supportActionBar!!.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        // TODO: Allow passing pictures directly here
        addPictures()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CHOOSE_PICTURE) {
            if (resultCode != RESULT_OK) {
                finish()
                return
            }

            mProgressBarFrame!!.visibility = View.VISIBLE

            doAsync {
                val addAction: Uri.() -> Unit = {
                    mUris.add(this)
                    mDisplayNames[this] = displayName(this)
                    mPreview[this] = getPreview(this)
                }

                if (data!!.clipData == null || data.clipData!!.itemCount == 0) {
                    // Only one picture is chosen
                    data.data?.apply(addAction)
                } else {
                    for (i in 0 until data.clipData!!.itemCount) {
                        data.clipData!!.getItemAt(i).uri.apply(addAction)
                    }
                }

                uiThread {
                    mProgressBarFrame!!.visibility = View.GONE
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun addPictures() = startActivityForResult(Intent().apply {
        action = Intent.ACTION_OPEN_DOCUMENT
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }, REQUEST_CHOOSE_PICTURE)

    data class SelectionViewHolder(
        val root: View,
        val title: TextView,
        val thumbnail: ImageView
    ): RecyclerView.ViewHolder(root)

    inner class SelectionAdapter: RecyclerView.Adapter<SelectionViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectionViewHolder {
            var title: TextView? = null
            var thumbnail: ImageView? = null
            val root = AnkoContext.create(this@ComposeActivity, parent).apply {
                relativeLayout {
                    backgroundResource = attr(android.R.attr.selectableItemBackground).resourceId
                    thumbnail = imageView {
                        id = ID_THUMBNAIL
                        scaleType = ImageView.ScaleType.CENTER_CROP
                    }.lparams {
                        width = dip(48)
                        height = dip(48)
                        margin = dip(10)
                    }

                    title = textView {
                        gravity = Gravity.CENTER_VERTICAL
                    }.lparams {
                        width = matchParent
                        height = dip(68)
                        rightOf(ID_THUMBNAIL)
                    }
                }
            }.view.apply {
                isClickable = true
            }

            return SelectionViewHolder(root, title!!, thumbnail!!)
        }

        override fun onBindViewHolder(holder: SelectionViewHolder, position: Int) {
            holder.apply {
                thumbnail.imageBitmap = mPreview[mUris[position]]
                title.text = mDisplayNames[mUris[position]]
            }
        }

        override fun getItemCount(): Int = mUris.size
    }
}