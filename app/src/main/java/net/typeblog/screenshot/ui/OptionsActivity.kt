package net.typeblog.screenshot.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Paint
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import net.typeblog.screenshot.R

import org.jetbrains.anko.*
import org.jetbrains.anko.custom.ankoView

class OptionsActivity: AppCompatActivity() {
    companion object {
        const val ID_SENSITIVITY = 33333
        const val ID_SKIP = 33334
        const val ID_SAMPLE_RATIO = 33335

        const val ID_SENSITIVITY_TEXT = 33336
        const val ID_SKIP_TEXT = 33337
        const val ID_SAMPLE_RATIO_TEXT = 33338
        const val ID_OPTIONS_NOTE = 33339

        const val ID_MENU_FINISH = 33400
    }

    private var mSensitivity: Float = 0.0f
    private var mSkip: Float = 0.0f
    private var mSampleRatio: Int = 0

    private lateinit var mImageView1: MyImageView
    private lateinit var mImageView2: MyImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mSensitivity = intent.getFloatExtra("sensitivity", 0f)
        mSkip = intent.getFloatExtra("skip", 0f)
        mSampleRatio = intent.getIntExtra("sample_ratio", 0)

        relativeLayout {
            appBar(this@OptionsActivity)
            linearLayout {
                orientation = LinearLayout.VERTICAL
                linearLayout {
                    orientation = LinearLayout.HORIZONTAL
                    mImageView1 = myImageView {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        imageURI = intent.getParcelableExtra("bmp1")
                        coverTop = mSkip
                    }.lparams {
                        width = matchParent
                        height = matchParent
                        weight = 1.0f
                    }

                    mImageView2 = myImageView {
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        imageURI = intent.getParcelableExtra("bmp2")
                        coverBottom = mSkip
                    }.lparams {
                        width = matchParent
                        height = matchParent
                        weight = 1.0f
                    }
                }.lparams {
                    width = matchParent
                    height = matchParent
                    weight = 0.4f
                }

                relativeLayout {
                    gravity = Gravity.CENTER
                    textView {
                        id = ID_OPTIONS_NOTE
                        textResource = R.string.options_note
                        gravity = Gravity.CENTER_HORIZONTAL
                        leftPadding = dip(80)
                        rightPadding = dip(80)
                    }.lparams {
                        width = matchParent
                        height = wrapContent
                    }

                    val seekBarSensitivity = seekBar {
                        id = ID_SENSITIVITY
                        max = 100
                        min = 60
                        progress = (mSensitivity * 100).toInt()
                        leftPadding = dip(80)
                        rightPadding = dip(80)
                    }.lparams {
                        width = matchParent
                        height = wrapContent
                        topMargin = dip(10)
                        below(ID_OPTIONS_NOTE)
                    }

                    textView {
                        id = ID_SENSITIVITY_TEXT
                        text = getString(R.string.sensitivity_text, mSensitivity)
                        gravity = Gravity.CENTER_HORIZONTAL
                    }.lparams {
                        width = matchParent
                        height = wrapContent
                        below(ID_SENSITIVITY)
                    }.also {
                        seekBarSensitivity.setOnSeekBarChangeListener(
                            MySeekBarChangeListener(it, R.string.sensitivity_text) { v ->
                                (v.toFloat() / 100f).also { s -> mSensitivity = s }
                            })
                    }

                    val seekBarSkip = seekBar {
                        id = ID_SKIP
                        max = 70
                        min = 30
                        progress = (mSkip * 100).toInt()
                        leftPadding = dip(80)
                        rightPadding = dip(80)
                    }.lparams {
                        width = matchParent
                        height = wrapContent
                        topMargin = dip(10)
                        below(ID_SENSITIVITY_TEXT)
                    }

                    textView {
                        id = ID_SKIP_TEXT
                        text = getString(R.string.skip_text, mSkip)
                        gravity = Gravity.CENTER_HORIZONTAL
                    }.lparams {
                        width = matchParent
                        height = wrapContent
                        below(ID_SKIP)
                    }.also {
                        seekBarSkip.setOnSeekBarChangeListener(
                            MySeekBarChangeListener(it, R.string.skip_text) { v ->
                                (v.toFloat() / 100f).also { s ->
                                    mSkip = s
                                    mImageView1.coverTop = mSkip
                                    mImageView2.coverBottom = mSkip
                                }
                            })
                    }

                    val seekBarSampleRatio = seekBar {
                        id = ID_SAMPLE_RATIO
                        max = 8
                        min = 1
                        progress = mSampleRatio
                        leftPadding = dip(80)
                        rightPadding = dip(80)
                    }.lparams {
                        width = matchParent
                        height = wrapContent
                        topMargin = dip(10)
                        below(ID_SKIP_TEXT)
                    }

                    textView {
                        id = ID_SAMPLE_RATIO_TEXT
                        text = getString(R.string.sample_ratio_text, mSampleRatio.toFloat())
                        gravity = Gravity.CENTER_HORIZONTAL
                    }.lparams {
                        width = matchParent
                        height = wrapContent
                        below(ID_SAMPLE_RATIO)
                    }.also {
                        seekBarSampleRatio.setOnSeekBarChangeListener(
                            MySeekBarChangeListener(it, R.string.sample_ratio_text) { v ->
                                mSampleRatio = v
                                v.toFloat()
                            })
                    }
                }.lparams {
                    width = matchParent
                    height = matchParent
                    weight = 0.6f
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
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menu!!.add(0, ID_MENU_FINISH, ID_MENU_FINISH, R.string.ok).apply {
            icon = getDrawable(R.drawable.ic_check_black_24dp)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            ID_MENU_FINISH -> {
                setResult(RESULT_OK, Intent().apply {
                    putExtra("sensitivity", mSensitivity)
                    putExtra("skip", mSkip)
                    putExtra("sample_ratio", mSampleRatio)
                })
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    inner class MySeekBarChangeListener(
        private val mTxtView: TextView,
        private val mTxtRes: Int,
        private val mFnVal: (Int) -> Float
    ): SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
            val myVal = mFnVal(progress)
            mTxtView.text = getString(mTxtRes, myVal)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar?) {
        }

        override fun onStopTrackingTouch(seekBar: SeekBar?) {
        }
    }

    private fun ViewManager.myImageView(init: MyImageView.() -> Unit): MyImageView =
        ankoView({ MyImageView(it, getColor(R.color.colorAccent)) }, 0, init)

    @SuppressLint("ViewConstructor")
    private class MyImageView(context: Context, coverColor: Int): ImageView(context) {
        var coverTop: Float = 0.0f
            set(v) {
                field = v
                invalidate()
            }
        var coverBottom: Float = 0.0f
            set(v) {
                field = v
                invalidate()
            }
        var coverPaint: Paint = Paint().apply {
            color = coverColor
            alpha = 50
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)

            canvas!!.drawRect(0f, 0f, measuredWidth.toFloat(),
                coverTop * measuredHeight, coverPaint)
            canvas.drawRect(0f, (1 - coverBottom) * measuredHeight,
                measuredWidth.toFloat(), measuredHeight.toFloat(), coverPaint)
        }
    }
}