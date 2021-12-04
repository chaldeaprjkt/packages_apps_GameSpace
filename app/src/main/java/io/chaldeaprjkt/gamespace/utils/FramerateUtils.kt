package io.chaldeaprjkt.gamespace.utils

import android.app.ActivityTaskManager
import android.view.SurfaceControlFpsListener

/**
 * Utilities to listen for sampling the frames per second for
 * SurfaceControl and its children.
 *
 */
object FrameRateUtils {
    private var isBound = false
    private lateinit var listener: SurfaceControlFpsListener

    private fun listenerFactory(ev: (Float) -> Unit) = object : SurfaceControlFpsListener() {
        override fun onFpsReported(fps: Float) {
            if (isBound) ev.invoke(fps)
        }
    }

    fun bind(task: ActivityTaskManager.RootTaskInfo?, onFpsReported: (Float) -> Unit) {
        if (isBound) return
        task?.taskId?.let {
            listener = listenerFactory(onFpsReported)
            listener.register(it)
            isBound = true
        } ?: run {
            isBound = false
        }
    }

    fun unbind() {
        if (::listener.isInitialized && isBound) {
            listener.unregister()
            isBound = false
        }
    }
}
