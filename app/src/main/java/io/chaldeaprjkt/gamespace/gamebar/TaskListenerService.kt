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

import android.app.ActivityTaskManager
import android.app.Service
import android.app.TaskStackListener
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log
import io.chaldeaprjkt.gamespace.data.SystemSettings


class TaskListenerService : Service() {
    private val taskManager by lazy { ActivityTaskManager.getService() }
    private val settings by lazy { SystemSettings(applicationContext) }

    private val listener by lazy {
        object : TaskStackListener() {
            override fun onTaskStackChanged() {
                checkTaskStack(taskManager?.focusedRootTaskInfo)
            }
        }
    }

    private var previousApp = UNKNOWN_APP
    private var isInGameMode = false

    override fun onCreate() {
        super.onCreate()
        isRunning = true
        try {
            taskManager.registerTaskStackListener(listener)
        } catch (e: RemoteException) {
            Log.d(TAG, e.toString())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        taskManager.unregisterTaskStackListener(listener)
        isRunning = false
        super.onDestroy()
    }

    private fun isGame(packageName: String) =
        settings.userGames.any { it.packageName == packageName }

    private fun checkTaskStack(info: ActivityTaskManager.RootTaskInfo?) {
        try {
            val app = info?.topActivity?.packageName ?: return
            if (app == previousApp && isInGameMode) return
            if (isGame(app)) {
                Intent(GameBroadcastReceiver.GAME_START).apply {
                    setPackage(packageName)
                    putExtra(SessionService.EXTRA_PACKAGE_NAME, app)
                    sendBroadcastAsUser(this, UserHandle.CURRENT)
                }
                isInGameMode = true
            } else if (isInGameMode) {
                Intent(GameBroadcastReceiver.GAME_STOP).apply {
                    setPackage(packageName)
                    sendBroadcastAsUser(this, UserHandle.CURRENT)
                }

                isInGameMode = false
            }

            previousApp = app
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }
    }

    companion object {
        const val TAG = "TaskListener"
        const val UNKNOWN_APP = "unknown"
        var isRunning = false
            private set

        fun start(context: Context) = Intent(context, TaskListenerService::class.java)
            .takeIf { !isRunning }
            ?.run { context.startServiceAsUser(this, UserHandle.CURRENT) }
    }
}
