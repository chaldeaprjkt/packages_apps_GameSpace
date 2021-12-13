package io.chaldeaprjkt.gamespace.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.utils.dp
import kotlin.math.roundToInt

class MenuSwitcher @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.bar_menu_switcher, this, true)
    }

    private val appSettings by lazy { AppSettings(context) }

    private val content: TextView?
        get() = findViewById(R.id.menu_content)

    private var lastFpsValue = 0f

    var showFps = false
        set(value) {
            if (!value) updateFpsValue(0f)
            layoutParams.width = if (value) LayoutParams.WRAP_CONTENT else 36.dp
            field = value
        }

    var isDragged = false
        set(value) {
            if (value && !showFps) setMenuIcon(R.drawable.ic_drag)
            field = value
        }

    fun updateFpsValue(newValue: Float) {
        if (lastFpsValue == newValue) return
        content?.text = newValue.roundToInt().toString()
        lastFpsValue = newValue
    }

    fun updateIconState(isExpanded: Boolean, location: Int) {
        showFps = if (isExpanded) false else appSettings.showFps
        when {
            isExpanded -> R.drawable.ic_close
            location > 0 -> R.drawable.ic_arrow_right
            else -> R.drawable.ic_arrow_left
        }.let { setMenuIcon(it) }
    }

    private fun setMenuIcon(icon: Int = R.drawable.ic_close) {
        val ic = if (showFps) null else resources.getDrawable(icon, context.theme)
        content?.textScaleX = if (showFps) 1f else 0f
        content?.setCompoundDrawablesRelativeWithIntrinsicBounds(null, ic, null, null)
    }
}
