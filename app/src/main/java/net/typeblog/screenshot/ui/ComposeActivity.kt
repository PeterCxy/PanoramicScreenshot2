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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import net.typeblog.screenshot.R

import net.typeblog.screenshot.util.*

import org.jetbrains.anko.*
import org.jetbrains.anko.recyclerview.v7.*
import org.jetbrains.anko.sdk27.coroutines.onClick
import org.jetbrains.anko.sdk27.coroutines.onTouch
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ComposeActivity: AppCompatActivity() {
    companion object {
        const val REQUEST_CHOOSE_PICTURE = 1000
        const val ID_THUMBNAIL = 102333
    }

    private val mUris = ArrayList<Uri>()
    private val mDisplayNames = HashMap<Uri, String?>()
    private val mPreview = HashMap<Uri, Bitmap?>()
    private val mAdapter = SelectionAdapter()
    private val mItemTouchHelper = ItemTouchHelper(ItemMoveHandler())

    private lateinit var mProgressBarFrame: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        relativeLayout {
            appBar(this@ComposeActivity)
            mItemTouchHelper.attachToRecyclerView(recyclerView {
                topPadding = dip(10)
            }.lparams {
                width = matchParent
                height = matchParent
                below(ID_APPBAR)
            }.apply {
                adapter = mAdapter
                layoutManager = LinearLayoutManager(this@ComposeActivity)
            })

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

            mProgressBarFrame.visibility = View.VISIBLE

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
                    mProgressBarFrame.visibility = View.GONE
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
        val child: View,
        val title: TextView,
        val thumbnail: ImageView
    ): RecyclerView.ViewHolder(root) {
        fun select() {
            // Set root background to null to avoid conflicting with child
            root.background = null
            child.backgroundColor = root.context.getColor(R.color.colorSelected)
            // To show the shadow of child (that's why we need this wrapper)
            root.verticalPadding = root.context.dip(4)
        }

        fun deselect() {
            // Restore everything
            root.backgroundResource =
                root.context.attr(android.R.attr.selectableItemBackground).resourceId
            child.backgroundResource = 0
            root.verticalPadding = 0
        }
    }

    inner class SelectionAdapter: RecyclerView.Adapter<SelectionViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SelectionViewHolder {
            lateinit var title: TextView
            lateinit var thumbnail: ImageView
            lateinit var reorder: ImageView
            lateinit var child: View
            val root = AnkoContext.create(this@ComposeActivity, parent).apply {
                relativeLayout {
                    backgroundResource = attr(android.R.attr.selectableItemBackground).resourceId
                    clipToPadding = false
                    child = relativeLayout {
                        elevation = dip(4).toFloat()
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

                        reorder = imageView {
                            imageResource = R.drawable.ic_reorder_black_24dp
                            isClickable = true
                        }.lparams {
                            width = wrapContent
                            height = wrapContent
                            alignParentRight()
                            centerVertically()
                            rightMargin = dip(10)
                        }
                    }
                }
            }.view.apply {
                isClickable = true
            }

            return SelectionViewHolder(root, child, title, thumbnail).apply {
                reorder.onTouch { _, _ ->
                    mItemTouchHelper.startDrag(this@apply)
                }
            }
        }

        override fun onBindViewHolder(holder: SelectionViewHolder, position: Int) {
            holder.apply {
                thumbnail.imageBitmap = mPreview[mUris[position]]
                title.text = mDisplayNames[mUris[position]]
            }
        }

        override fun getItemCount(): Int = mUris.size
    }

    inner class ItemMoveHandler: ItemTouchHelper.Callback() {
        override fun isLongPressDragEnabled(): Boolean = true
        override fun isItemViewSwipeEnabled(): Boolean = false

        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ): Int = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0)

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            if (from < to) {
                for (i in from until to) {
                    Collections.swap(mUris, i, i + 1)
                }
            } else {
                for (i in from downTo (to + 1)) {
                    Collections.swap(mUris, i, i - 1)
                }
            }
            mAdapter.notifyItemMoved(from, to)
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // Not Supported
        }

        override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
            if (actionState != ItemTouchHelper.ACTION_STATE_IDLE) {
                (viewHolder as SelectionViewHolder).select()
            }
            super.onSelectedChanged(viewHolder, actionState)
        }

        override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
            (viewHolder as SelectionViewHolder).deselect()
            super.clearView(recyclerView, viewHolder)
        }
    }
}