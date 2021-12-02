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
package io.chaldeaprjkt.gamespace.utils

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.view.WindowManager
import com.android.internal.util.ScreenshotHelper
import com.android.systemui.screenrecord.IRemoteRecording
import java.util.function.Consumer
import kotlin.system.exitProcess

object ScreenUtils {
    @SuppressLint("StaticFieldLeak")  // We store the application context, not an activity.
    private var helper: ScreenshotHelper? = null
    private var remoteRecording: IRemoteRecording? = null
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            try {
                remoteRecording = IRemoteRecording.Stub.asInterface(service)
            } catch (e: Exception) {
                e.printStackTrace()
                exitProcess(1)
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            remoteRecording = null
        }
    }

    val recorder: IRemoteRecording? get() = remoteRecording

    fun bind(context: Context) {
        if (helper != null) return
        helper = ScreenshotHelper(context.applicationContext)
        val recordingServiceIntent =
            Intent().apply {
                component = ComponentName(
                    "com.android.systemui",
                    "com.android.systemui.screenrecord.RecordingService"
                )
            }
        context.applicationContext.bindService(
            recordingServiceIntent,
            serviceConnection,
            Context.BIND_AUTO_CREATE
        ).let { if (!it) exitProcess(1) }
    }

    fun unbind(context: Context) {
        context.applicationContext.unbindService(serviceConnection)
        remoteRecording = null
    }

    fun takeScreenshot(handler: Handler, consumer: Consumer<Uri>? = null) {
        helper?.takeScreenshot(
            WindowManager.TAKE_SCREENSHOT_FULLSCREEN, true, true,
            WindowManager.ScreenshotSource.SCREENSHOT_GLOBAL_ACTIONS, handler, consumer
        )
    }
}
