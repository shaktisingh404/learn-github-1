package com.brainpal.counter

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Black pill drawn over the top-centre of the screen. A transparent gap in the
 * middle sits under the camera cut-out; the counts sit on either side of it.
 */
class NotchOverlay(private val context: Context) {
    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var leftLabel: TextView? = null
    private var rightLabel: TextView? = null
    private var root: LinearLayout? = null

    private fun dp(v: Int) = (v * context.resources.displayMetrics.density).toInt()

    private fun label() = TextView(context).apply {
        setTextColor(Color.WHITE)
        textSize = 11f
        setPadding(dp(8), 0, dp(8), 0)
        gravity = Gravity.CENTER
    }

    private fun cutoutSize(): Pair<Int, Int> {
        if (Build.VERSION.SDK_INT >= 30) {
            val cut = wm.currentWindowMetrics.windowInsets.displayCutout
            val rect = cut?.boundingRectTop
            if (rect != null && !rect.isEmpty) return rect.width() to rect.height()
        }
        val id = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        val bar = if (id > 0) context.resources.getDimensionPixelSize(id) else dp(24)
        return dp(72) to bar
    }

    fun show(ig: Int, yt: Int) {
        update(ig, yt)
        if (root != null) return
        val (cutW, cutH) = cutoutSize()
        // Fresh views each time: a view removed from the window still keeps its parent.
        val l = label()
        val r = label()
        val gap = View(context).apply { layoutParams = LinearLayout.LayoutParams(cutW, 1) }
        leftLabel = l
        rightLabel = r
        val pill = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.BLACK)
                cornerRadius = cutH / 2f
            }
            addView(l)
            addView(gap)
            addView(r)
        }
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            cutH,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        wm.addView(pill, lp)
        root = pill
        update(ig, yt)
    }

    fun update(ig: Int, yt: Int) {
        leftLabel?.text = "▶ $ig"
        rightLabel?.text = "▷ $yt"
    }

    fun hide() {
        root?.let { runCatching { wm.removeView(it) } }
        root = null
        leftLabel = null
        rightLabel = null
    }
}
