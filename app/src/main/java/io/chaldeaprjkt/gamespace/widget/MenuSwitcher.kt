package io.chaldeaprjkt.gamespace.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.doOnLayout
import androidx.core.view.doOnPreDraw
import androidx.core.view.isVisible
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.utils.dp
import kotlin.math.roundToInt

class MenuSwitcher : LinearLayout {

    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet?, dsAttr: Int) : super(ctx, attrs, dsAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.bar_menu_switcher, this, true)
    }

    private val appSettings by lazy { AppSettings(context) }

    private val titleView: TextView?
        get() = findViewById(R.id.text)

    private val iconView: ImageView?
        get() = findViewById(R.id.icon)

    private var lastFpsValue = 0f

    var icon = R.drawable.ic_arrow_right
        set(value) {
            iconView?.setImageResource(value)
            field = value
        }

    var showFps = false
        set(value) {
            titleView?.isVisible = value
            iconView?.isVisible = !value
            if (!showFps) {
                updateFpsValue(0f)
            }
            layoutParams.width = if (value) LayoutParams.WRAP_CONTENT else 36.dp
            field = value
        }

    var isDragged = false
        set(value) {
            if (value)
                icon = R.drawable.ic_drag
            field = value
        }

    fun updateFpsValue(newValue: Float) {
        if (lastFpsValue == newValue) return
        if (newValue < 1) {
            titleView?.text = ""
        } else {
            titleView?.text = newValue.roundToInt().toString()
        }
    }

    fun updateIconState(isExpanded: Boolean, location: Int) {
        showFps = if (isExpanded) false else appSettings.showFps
        icon = when {
            isExpanded -> {
                R.drawable.ic_close
            }
            location > 0 -> R.drawable.ic_arrow_right
            else -> R.drawable.ic_arrow_left
        }
    }
}