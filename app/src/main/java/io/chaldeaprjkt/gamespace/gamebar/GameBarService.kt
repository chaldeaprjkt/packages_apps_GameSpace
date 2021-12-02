/*
 * Copyright (C) 2021 Chaldeaprjkt
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.chaldeaprjkt.gamespace.gamebar

import android.app.Service
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.graphics.Point
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.view.*
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.view.*
import com.android.systemui.screenrecord.IRecordingCallback
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.SessionState
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.utils.ScreenUtils
import io.chaldeaprjkt.gamespace.utils.dp2px
import io.chaldeaprjkt.gamespace.utils.getStatusBarHeight
import io.chaldeaprjkt.gamespace.utils.registerDraggableTouchListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min


class GameBarService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.Main)
    private val wm by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    private val settings by lazy { SystemSettings(applicationContext) }
    private val appSettings by lazy { AppSettings(applicationContext) }
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val session by lazy { SessionState() }
    private val themedInflater by lazy {
        LayoutInflater.from(this)
            .cloneInContext(ContextThemeWrapper(this, R.style.GameSpaceTheme))
    }
    private val windowParams by lazy {
        WindowManager.LayoutParams(
            WindowManager.LayoutParams.TYPE_SYSTEM_DIALOG,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            width = WindowManager.LayoutParams.WRAP_CONTENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            preferMinimalPostProcessing = true
            gravity = Gravity.TOP
        }
    }

    private lateinit var rootView: View
    private lateinit var container: LinearLayout
    private lateinit var menuSwitcher: ImageButton
    private val binder = GameBarBinder()
    private val firstPaint = Runnable { initActions() }
    private var barExpanded: Boolean = false
        set(value) {
            field = value
            menuSwitcher.isSelected = value
            container.children.forEach {
                if (it.id != R.id.action_menu_switcher) {
                    it.isVisible = value
                }
            }
            updateBackground()
            updateExpanderMenu()
            updateContainerGaps()
        }

    override fun onCreate() {
        super.onCreate()
        rootView = themedInflater.inflate(R.layout.window_util, FrameLayout(this), false)
        container = rootView.findViewById(R.id.container_bar)
        menuSwitcher = rootView.findViewById(R.id.action_menu_switcher)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> onActionStop()
            ACTION_START -> onActionStart()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent) = binder

    inner class GameBarBinder : Binder() {
        fun getService() = this@GameBarService
    }

    override fun onDestroy() {
        onActionStop()
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (!rootView.isVisible) {
            handler.removeCallbacks(firstPaint)
            handler.postDelayed({
                firstPaint.run()
                dockCollapsedMenu()
            }, 100)
        } else {
            dockCollapsedMenu()
        }
    }

    // for client service
    fun onGameStart() = scope.launch { onActionStart() }
    fun onGameLeave() = scope.launch { onActionStop() }

    private fun onActionStart() {
        applyCustomSettings()
        rootView.isVisible = false
        rootView.alpha = 0f
        if (!rootView.isAttachedToWindow) {
            wm.addView(rootView, windowParams)
        }
        handler.postDelayed(firstPaint, 500)
    }

    private fun onActionStop() {
        restoreCustomSettings()
        if (rootView.isAttachedToWindow) {
            wm.removeViewImmediate(rootView)
        }
    }

    private fun applyCustomSettings() {
        session.headsUp = settings.systemHeadsUp
        session.autoBrightness = settings.autoBrightness
        session.threeScreenshot = settings.threeScreenshot
        if (settings.userNoHeadsUp) {
            settings.systemHeadsUp = false
        }
        if (settings.userNoAutoBrightness) {
            settings.autoBrightness = false
        }
        if (settings.userNoThreeScreenshot) {
            settings.threeScreenshot = false
        }
    }

    private fun restoreCustomSettings() {
        if (settings.userNoHeadsUp) {
            session.headsUp?.let { settings.systemHeadsUp = it }
        }
        if (settings.userNoAutoBrightness) {
            session.autoBrightness?.let { settings.autoBrightness = it }
        }
        if (settings.userNoThreeScreenshot) {
            session.threeScreenshot?.let { settings.threeScreenshot = it }
        }
    }

    private fun updateLayout(with: (WindowManager.LayoutParams) -> Unit = {}) {
        if (rootView.isAttachedToWindow) {
            wm.updateViewLayout(rootView, windowParams.apply(with))
        }
    }

    private fun initActions() {
        rootView.isVisible = true
        rootView.animate()
            .alpha(1f)
            .apply { duration = 300 }
            .start()
        barExpanded = false
        windowParams.x = appSettings.x
        windowParams.y = appSettings.y
        dockCollapsedMenu()

        menuSwitcherButton()
        screenshotButton()
        headsUpButton()
        recorderButton()
    }

    private fun onBarDragged(dragged: Boolean) {
        container.isSelected = dragged
        updateBackground()
        if (dragged) {
            menuSwitcher.setImageResource(R.drawable.ic_drag_handle)
            container.translationX = 0f
        } else {
            menuSwitcher.setImageResource(R.drawable.ic_action_arrow)
        }
    }

    private fun updateBackground() {
        container.setBackgroundResource(
            when {
                barExpanded -> R.drawable.bar_expanded
                windowParams.x < 0 -> R.drawable.bar_collapsed_start
                else -> R.drawable.bar_collapsed_end
            }
        )
    }

    private fun updateContainerGaps() {
        if (barExpanded) {
            container.updatePadding(16, 16, 16, 16)
            (container.layoutParams as ViewGroup.MarginLayoutParams)
                .updateMargins(right = 48, left = 48)
        } else {
            container.updatePadding(0, 0, 0, 0)
            (container.layoutParams as ViewGroup.MarginLayoutParams)
                .updateMargins(right = 0, left = 0)
        }
    }

    private fun updateExpanderMenu() {
        if ((barExpanded && windowParams.x > 0) || (!barExpanded && windowParams.x < 0)) {
            menuSwitcher.rotation = 90f
        } else {
            menuSwitcher.rotation = -90f
        }
    }

    private fun dockCollapsedMenu() {
        val halfWidth = wm.maximumWindowMetrics.bounds.width() / 2
        updateBackground()
        updateExpanderMenu()
        updateContainerGaps()
        if (windowParams.x < 0) {
            container.translationX = -22f
            windowParams.x = -halfWidth
        } else {
            container.translationX = 22f
            windowParams.x = halfWidth
        }

        val safeArea = getStatusBarHeight()?.plus(4.dp2px) ?: 32.dp2px
        val safeHeight = wm.maximumWindowMetrics.bounds.height() - safeArea
        windowParams.y = max(min(windowParams.y, safeHeight), safeArea)
        updateLayout()
    }

    private fun takeShot() {
        val afterShot: () -> Unit = {
            barExpanded = false
            handler.postDelayed({
                updateLayout { it.alpha = 1f }
            }, 100)
        }

        updateLayout { it.alpha = 0f }
        handler.postDelayed({
            try {
                ScreenUtils.takeScreenshot(handler) {
                    afterShot()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                afterShot()
            }
        }, 250)
    }

    private fun menuSwitcherButton() {
        menuSwitcher.setOnClickListener {
            barExpanded = !barExpanded
        }
        menuSwitcher.registerDraggableTouchListener(
            initPoint = { Point(windowParams.x, windowParams.y) },
            listener = { x, y ->
                onBarDragged(true)
                windowParams.x = x
                windowParams.y = y
                updateLayout()
            },
            onComplete = {
                onBarDragged(false)
                dockCollapsedMenu()
                appSettings.x = windowParams.x
                appSettings.y = windowParams.y
            }
        )
    }

    private fun screenshotButton() {
        val actionScreenshot = rootView.findViewById<ImageButton>(R.id.action_screenshot)
        actionScreenshot.setOnClickListener {
            takeShot()
        }
    }

    private fun recorderButton() {
        val actionRecorder = rootView.findViewById<ImageButton>(R.id.action_record)
        val recorder = ScreenUtils.recorder ?: let { actionRecorder.isVisible = false; return }
        recorder.addRecordingCallback(object : IRecordingCallback.Stub() {
            override fun onRecordingStart() {
                handler.post {
                    actionRecorder.isSelected = true
                }
            }

            override fun onRecordingEnd() {
                handler.post {
                    actionRecorder.isSelected = false
                }
            }
        })
        actionRecorder.setOnClickListener {
            if (recorder.isStarting) {
                return@setOnClickListener
            }

            if (!recorder.isRecording) {
                recorder.startRecording()
            } else {
                recorder.stopRecording()
            }

            barExpanded = false
        }
    }

    private fun headsUpButton() {
        val actionsHeadsUp = rootView.findViewById<ImageButton>(R.id.action_heads_up)
        actionsHeadsUp.isSelected = settings.systemHeadsUp
        actionsHeadsUp.setOnClickListener {
            actionsHeadsUp.isSelected = !actionsHeadsUp.isSelected
            settings.systemHeadsUp = actionsHeadsUp.isSelected
            val stateText = if (actionsHeadsUp.isSelected) "enabled" else "disabled"
            Toast.makeText(it.context, "Heads up is $stateText", Toast.LENGTH_SHORT)
                .show()
        }
    }

    companion object {
        const val TAG = "GameBar"
        const val ACTION_START = "GameBar.ACTION_START"
        const val ACTION_STOP = "GameBar.ACTION_STOP"
    }
}
