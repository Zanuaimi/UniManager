package com.zanuaimi.unimanager

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.view.animation.DecelerateInterpolator

object UiKit {
    val background = Color.rgb(33, 0, 0)
    val surface = Color.rgb(48, 0, 0)
    val elevated = Color.rgb(80, 0, 0)
    val accent = Color.rgb(255, 86, 86)
    val accentDark = Color.rgb(170, 0, 0)
    val text = Color.WHITE
    val muted = Color.rgb(232, 186, 186)
    val subtle = Color.argb(175, 232, 186, 186)
    val outline = Color.rgb(125, 54, 54)

    fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    fun rounded(color: Int, radius: Int = 18, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius.toFloat()
            stroke?.let { setStroke(1, it) }
        }

    fun text(context: Context, value: String, size: Float, color: Int = text): TextView =
        TextView(context).apply {
            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = true
        }

    fun spacing(context: Context, height: Int): View = View(context).apply {
        layoutParams = ViewGroup.LayoutParams(1, context.dp(height))
    }

    fun animateAppear(view: View, delay: Long = 0L) {
        view.animate().cancel()
        view.alpha = 0f
        view.translationY = 18f
        view.scaleX = 0.98f
        view.scaleY = 0.98f
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(delay)
            .setDuration(260)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    fun animatePress(view: View) {
        view.animate().cancel()
        view.animate().scaleX(0.96f).scaleY(0.96f).setDuration(70).withEndAction {
            view.animate().cancel()
            view.animate().scaleX(1f).scaleY(1f).setDuration(140).start()
        }.start()
    }
}
