package io.chaldeaprjkt.gamespace.widget

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isVisible
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.utils.dp

class MenuSwitcher : LinearLayout {
    constructor(ctx: Context) : super(ctx)
    constructor(ctx: Context, attrs: AttributeSet?) : super(ctx, attrs)
    constructor(ctx: Context, attrs: AttributeSet?, dsAttr: Int) : super(ctx, attrs, dsAttr)

    init {
        LayoutInflater.from(context).inflate(R.layout.bar_menu_switcher, this, true)
    }

    private val titleView: TextView?
        get() = findViewById(R.id.text)

    private val iconView: ImageView?
        get() = findViewById(R.id.icon)

    var icon = R.drawable.ic_action_arrow
        set(value) {
            iconView?.setImageResource(value)
            field = value
        }
    var text = ""
        set(value) {
            titleView?.text = value
            field = value
        }

    var showFps = false
        set(value) {
            titleView?.isVisible = value
            iconView?.isVisible = !value
            if (!showFps) {
                text = ""
            }
            layoutParams.width = if (value) LayoutParams.WRAP_CONTENT else 36.dp
            field = value
        }

    var iconRotation = 0f
        set(value) {
            iconView?.rotation = value
            field = value
        }

    var isDragged = false
        set(value) {
            icon = if (value) R.drawable.ic_drag_handle else R.drawable.ic_action_arrow
            field = value
        }
}