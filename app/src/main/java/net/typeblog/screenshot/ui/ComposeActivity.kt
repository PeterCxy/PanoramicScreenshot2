package net.typeblog.screenshot.ui

import android.app.ActivityOptions
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import net.typeblog.screenshot.R
import net.typeblog.screenshot.util.*

import org.jetbrains.anko.*
import org.jetbrains.anko.design.floatingActionButton
import org.jetbrains.anko.recyclerview.v7.*
import org.jetbrains.anko.sdk27.coroutines.*

import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class ComposeActivity: AppCompatActivity() {
    companion object {
        const val REQUEST_CHOOSE_PICTURE = 1000
        const val REQUEST_SETTINGS = 1001
        const val ID_THUMBNAIL = 102333
        const val ID_REORDER = 102334

        // Menu IDs
        const val ID_SETTINGS = 112336
        const val ID_ADD = 112337
    }

    private val mUris = ArrayList<Uri>()
    private val mDisplayNames = HashMap<Uri, String?>()
    private val mPreview = HashMap<Uri, Bitmap?>()
    private val mAdapter = SelectionAdapter()
    private val mItemTouchHelper = ItemTouchHelper(ItemMoveHandler())

    private lateinit var mProgressBarFrame: FrameLayout

    // TODO: Make these permanent in SharedPreferences?
    private var mSensitivity: Float = 0.95f
    private var mSkip: Float = 0.5f
    private var mSampleRatio: Int = 4

    val mAddAction: Uri.() -> Unit = {
        mUris.add(this)
        mDisplayNames[this] = displayName(this)
        mPreview[this] = getPreview(this)
    }

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

            floatingActionButton {
                imageResource = R.drawable.ic_check_white_24dp
            }.lparams {
                width = wrapContent
                height = wrapContent
                margin = dip(16)
                alignParentEnd()
                alignParentBottom()
            }.onClick { doCompose() }
        }

        supportActionBar!!.apply {
            setDisplayHomeAsUpEnabled(true)
        }

        val uris = intent.getParcelableArrayListExtra<Uri>("uris")
        if (uris == null) {
            addPictures()
        } else {
            mProgressBarFrame.visibility = View.VISIBLE

            doAsync {
                uris.forEach(mAddAction)

                uiThread {
                    mProgressBarFrame.visibility = View.GONE
                    mAdapter.notifyDataSetChanged()
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CHOOSE_PICTURE && resultCode == RESULT_OK) {
            mProgressBarFrame.visibility = View.VISIBLE

            doAsync {
                if (data!!.clipData == null || data.clipData!!.itemCount == 0) {
                    // Only one picture is chosen
                    data.data?.apply(mAddAction)
                } else {
                    for (i in 0 until data.clipData!!.itemCount) {
                        data.clipData!!.getItemAt(i).uri.apply(mAddAction)
                    }
                }

                uiThread {
                    mProgressBarFrame.visibility = View.GONE
                    mAdapter.notifyDataSetChanged()
                }
            }
        } else if (requestCode == REQUEST_SETTINGS && resultCode == RESULT_OK) {
            mSensitivity = data!!.getFloatExtra("sensitivity", mSensitivity)
            mSkip = data.getFloatExtra("skip", mSkip)
            mSampleRatio = data.getIntExtra("sample_ratio", mSampleRatio)
        }
    }

    private fun addPictures() = startActivityForResult(Intent().apply {
        action = Intent.ACTION_OPEN_DOCUMENT
        addCategory(Intent.CATEGORY_OPENABLE)
        type = "image/*"
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
    }, REQUEST_CHOOSE_PICTURE)

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu!!.add(0, ID_SETTINGS, 1, R.string.options).apply {
            icon = getDrawable(R.drawable.ic_settings_black_24dp)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        menu.add(0, ID_ADD, 0, R.string.add).apply {
            icon = getDrawable(R.drawable.ic_add_black_24dp)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            ID_SETTINGS -> {
                doSettings()
                return true
            }
            ID_ADD -> {
                addPictures()
                return true
            }
        }
        return false
    }

    private fun assertPicNum(fn: () -> Unit) {
        if (mUris.size < 2) {
            Toast.makeText(this, R.string.not_enough_pictures, Toast.LENGTH_SHORT).show()
            return
        }

        fn()
    }

    private fun doCompose() {
        assertPicNum {
            ComposeProgressDialogFragment(mUris, mSensitivity, mSkip, mSampleRatio)
                .show(supportFragmentManager, "PROGRESS")
        }
    }

    private fun doSettings() {
        assertPicNum {
            startActivityForResult(Intent(this, OptionsActivity::class.java).apply {
                putExtra("sensitivity", mSensitivity)
                putExtra("skip", mSkip)
                putExtra("sample_ratio", mSampleRatio)
                putExtra("bmp1", mUris[0])
                putExtra("bmp2", mUris[1])
            }, REQUEST_SETTINGS)
        }
    }

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
            child.elevation = root.context.dip(2).toFloat()
        }

        fun deselect() {
            // Restore everything
            root.backgroundResource =
                root.context.attr(android.R.attr.selectableItemBackground).resourceId
            child.backgroundResource = 0
            child.elevation = 0f
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
                    // To show the shadow of child (that's why we need this wrapper)
                    verticalPadding = dip(4)
                    clipToPadding = false
                    child = relativeLayout {
                        thumbnail = imageView {
                            id = ID_THUMBNAIL
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }.lparams {
                            width = dip(96)
                            height = dip(96)
                            margin = dip(10)
                        }

                        reorder = imageView {
                            id = ID_REORDER
                            imageResource = R.drawable.ic_reorder_black_24dp
                            isClickable = true
                        }.lparams {
                            width = wrapContent
                            height = wrapContent
                            alignParentRight()
                            centerVertically()
                            rightMargin = dip(10)
                        }

                        title = textView {
                            gravity = Gravity.CENTER_VERTICAL
                        }.lparams {
                            width = matchParent
                            height = wrapContent
                            leftMargin = dip(10)
                            centerVertically()
                            rightOf(ID_THUMBNAIL)
                            leftOf(ID_REORDER)
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
                root.onClick {
                    ActivityOptions.makeSceneTransitionAnimation(
                        this@ComposeActivity, thumbnail, "image")
                        .also {
                            startActivity(Intent(this@ComposeActivity, ImageViewActivity::class.java).apply {
                                putExtra("picUri", mUris[position])
                            }, it.toBundle())
                        }
                }
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