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
import android.app.GameManager
import android.app.Service
import android.app.TaskStackListener
import android.content.*
import android.os.IBinder
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.SessionState
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import io.chaldeaprjkt.gamespace.utils.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch


class TaskListenerService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val taskManager by lazy { ActivityTaskManager.getService() }
    private val appSettings by lazy { AppSettings(applicationContext) }
    private val settings by lazy { SystemSettings(applicationContext) }
    private val listener by lazy {
        object : TaskStackListener() {
            override fun onTaskStackChanged() {
                checkTaskStack(taskManager?.focusedRootTaskInfo)
            }
        }
    }
    private val screenReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                previousApp = UNKNOWN_APP
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> {
                        // release it when user turn off the screen using power button
                        ScreenUtils.stayAwake(false)
                    }
                }
            }
        }
    }
    private val gameBarConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            gameBar = (service as GameBarService.GameBarBinder).getService()
            checkTaskStack(taskManager.focusedRootTaskInfo)
        }

        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    private var previousApp = UNKNOWN_APP
    private lateinit var gameBar: GameBarService
    private lateinit var gameManager: GameManager

    override fun onCreate() {
        isRunning = true
        try {
            taskManager.registerTaskStackListener(listener)
            ScreenUtils.bind(this)
        } catch (e: RemoteException) {
            Log.d(TAG, e.toString())
        }
        registerReceiver(screenReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        })
        gameManager = getSystemService(Context.GAME_SERVICE) as GameManager
        GameModeUtils.bind(gameManager)
        super.onCreate()
        Intent(this, GameBarService::class.java).apply {
            bindServiceAsUser(this, gameBarConnection, Context.BIND_AUTO_CREATE, UserHandle.CURRENT)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // restore settings in case the listener got destroyed gracefully
        if (session != null) {
            settings.restoreUserSettings(session)
        }
        GameModeUtils.unbind()
        unbindService(gameBarConnection)
        ScreenUtils.unbind(this)
        unregisterReceiver(screenReceiver)
        isRunning = false
        super.onDestroy()
    }

    private fun isGame(packageName: String) =
        settings.userGames.any { it.packageName == packageName }

    private var session: SessionState? = null

    private fun checkTaskStack(info: ActivityTaskManager.RootTaskInfo?) {
        try {
            val currentApp = info?.topActivity?.packageName ?: return
            if (currentApp == previousApp) return
            if (isGame(currentApp)) {
                if (session?.packageName == previousApp) {
                    // looks like user moving to other game
                    // restore the session first before applying new one
                    settings.restoreUserSettings(session)
                }
                session = SessionState(currentApp)
                settings.applyUserSettings(session)
                applyGameModeConfig(currentApp)
                gameBar.onGameStart()
                ScreenUtils.stayAwake(appSettings.stayAwake)
            } else if (session != null) {
                ScreenUtils.stayAwake(false)
                gameBar.onGameLeave()
                settings.restoreUserSettings(session)
                session = null
            }

            previousApp = currentApp
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }
    }

    private fun applyGameModeConfig(app: String) {
        val preferred = settings.userGames.firstOrNull { it.packageName == app }
            ?.mode ?: GameModeUtils.defaultPreferredMode
        GameModeUtils.activeGame = settings.userGames.firstOrNull { it.packageName == app }
        scope.launch {
            gameManager.getAvailableGameModes(app)
                .takeIf { it.contains(preferred) }
                ?.run { gameManager.setGameMode(app, preferred) }
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
