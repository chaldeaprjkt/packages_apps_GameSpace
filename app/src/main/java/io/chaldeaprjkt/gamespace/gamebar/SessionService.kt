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

import android.annotation.SuppressLint
import android.app.ActivityTaskManager
import android.app.GameManager
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.GameSession
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import io.chaldeaprjkt.gamespace.utils.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint(Service::class)
class SessionService : Hilt_SessionService() {
    @Inject
    lateinit var appSettings: AppSettings

    @Inject
    lateinit var settings: SystemSettings

    @Inject
    lateinit var session: GameSession

    @Inject
    lateinit var screenUtils: ScreenUtils

    @Inject
    lateinit var gameModeUtils: GameModeUtils

    private val scope = CoroutineScope(Job() + Dispatchers.IO)

    private val gameBarConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isBarConnected = true
            gameBar = (service as GameBarService.GameBarBinder).getService()
            onGameBarReady()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBarConnected = false
            stopSelf()
        }
    }

    private lateinit var commandIntent: Intent
    private lateinit var gameBar: GameBarService
    private lateinit var gameManager: GameManager
    private var isBarConnected = false

    @SuppressLint("WrongConstant")
    override fun onCreate() {
        super.onCreate()
        try {
            screenUtils.bind()
        } catch (e: RemoteException) {
            Log.d(TAG, e.toString())
        }
        gameManager = getSystemService(Context.GAME_SERVICE) as GameManager
        gameModeUtils.bind(gameManager)
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let { commandIntent = it }
        super.onStartCommand(intent, flags, startId)
        if (intent == null && flags == 0 && startId > 1) {
            return tryStartFromDeath()
        }

        when (intent?.action) {
            START -> startGameBar()
            STOP -> stopSelf()
        }
        return START_STICKY
    }

    private fun startGameBar() {
        Intent(this, GameBarService::class.java).apply {
            bindServiceAsUser(this, gameBarConnection, Context.BIND_AUTO_CREATE, UserHandle.CURRENT)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (isBarConnected) {
            gameBar.onGameLeave()
            unbindService(gameBarConnection)
        }
        session.unregister()
        gameModeUtils.unbind()
        screenUtils.unbind()
        isRunning = false
        super.onDestroy()
    }

    private fun onGameBarReady() {
        if (!isBarConnected) {
            startGameBar()
            return
        }

        try {
            session.unregister()
            if (!::commandIntent.isInitialized) {
                // something is not right, bailing out
                stopSelf()
            }
            val app = commandIntent.getStringExtra(EXTRA_PACKAGE_NAME)
            session.register(app)
            applyGameModeConfig(app)
            gameBar.onGameStart()
            screenUtils.stayAwake = appSettings.stayAwake
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }
    }

    private fun tryStartFromDeath(): Int {
        val game = ActivityTaskManager.getService()
            ?.focusedRootTaskInfo
            ?.topActivity?.packageName
            ?: return START_NOT_STICKY

        if (!settings.userGames.any { it.packageName == game }) {
            return START_NOT_STICKY
        }

        commandIntent = Intent(START).putExtra(EXTRA_PACKAGE_NAME, game)
        startGameBar()
        return START_STICKY
    }

    private fun applyGameModeConfig(app: String) {
        val preferred = settings.userGames.firstOrNull { it.packageName == app }
            ?.mode ?: GameModeUtils.defaultPreferredMode
        gameModeUtils.activeGame = settings.userGames.firstOrNull { it.packageName == app }
        scope.launch {
            gameManager.getAvailableGameModes(app)
                .takeIf { it.contains(preferred) }
                ?.run { gameManager.setGameMode(app, preferred) }
        }
    }

    companion object {
        const val TAG = "SessionService"
        const val START = "game_start"
        const val STOP = "game_stop"
        const val EXTRA_PACKAGE_NAME = "package_name"
        var isRunning = false
            private set

        fun start(context: Context, app: String) = Intent(context, SessionService::class.java)
            .apply {
                action = START
                putExtra(EXTRA_PACKAGE_NAME, app)
            }
            .takeIf { !isRunning }
            ?.run { context.startServiceAsUser(this, UserHandle.CURRENT) }

        fun stop(context: Context) = Intent(context, SessionService::class.java)
            .apply { action = STOP }
            .takeIf { isRunning }
            ?.run { context.startServiceAsUser(this, UserHandle.CURRENT) }
    }
}
