package io.chaldeaprjkt.gamespace.widget

import android.app.ActivityTaskManager
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.SurfaceControlFpsListener
import android.widget.LinearLayout
import android.widget.TextView
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.utils.di.ServiceViewEntryPoint
import io.chaldeaprjkt.gamespace.utils.dp
import io.chaldeaprjkt.gamespace.utils.entryPointOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.RoundingMode
import java.text.DecimalFormat

class MenuSwitcher @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : LinearLayout(context, attrs) {

    init {
        LayoutInflater.from(context).inflate(R.layout.bar_menu_switcher, this, true)
    }

    private val appSettings by lazy { context.entryPointOf<ServiceViewEntryPoint>().appSettings() }
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val taskManager by lazy { ActivityTaskManager.getService() }

    private val surfaceListener = object : SurfaceControlFpsListener() {
        override fun onFpsReported(fps: Float) {
            if (isAttachedToWindow) {
                onFrameUpdated(fps)
            }
        }
    }

    private val content: TextView?
        get() = findViewById(R.id.menu_content)

    var showFps = false
        set(value) {
            layoutParams.width = if (value) LayoutParams.WRAP_CONTENT else 36.dp
            field = value
        }

    var isDragged = false
        set(value) {
            if (value && !showFps) setMenuIcon(R.drawable.ic_drag)
            field = value
        }

    fun updateIconState(isExpanded: Boolean, location: Int) {
        showFps = if (isExpanded) false else appSettings.showFps
        when {
            isExpanded -> R.drawable.ic_close
            location > 0 -> R.drawable.ic_arrow_right
            else -> R.drawable.ic_arrow_left
        }.let { setMenuIcon(it) }
        updateFrameRateBinding()
    }

    private fun onFrameUpdated(newValue: Float) = scope.launch {
        DecimalFormat("#").apply {
            roundingMode = RoundingMode.HALF_EVEN
            content?.text = this.format(newValue)
        }
    }

    private fun updateFrameRateBinding() {
        if (showFps) {
            taskManager?.focusedRootTaskInfo?.taskId?.let { surfaceListener.register(it) }
        } else {
            surfaceListener.unregister()
        }
    }

    private fun setMenuIcon(icon: Int = R.drawable.ic_close) {
        val ic = if (showFps) null else resources.getDrawable(icon, context.theme)
        content?.textScaleX = if (showFps) 1f else 0f
        content?.setCompoundDrawablesRelativeWithIntrinsicBounds(null, ic, null, null)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        surfaceListener.unregister()
    }
}
