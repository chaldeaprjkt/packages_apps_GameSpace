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
import android.app.GameManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.os.IBinder
import android.os.RemoteException
import android.os.UserHandle
import android.util.Log
import io.chaldeaprjkt.gamespace.data.AppSettings
import io.chaldeaprjkt.gamespace.data.GameSession
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import io.chaldeaprjkt.gamespace.utils.ScreenUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SessionService : Service() {
    private val scope = CoroutineScope(Job() + Dispatchers.IO)
    private val appSettings by lazy { AppSettings(applicationContext) }
    private val settings by lazy { SystemSettings(applicationContext) }
    private val session by lazy { GameSession(applicationContext) }

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
            onGameBarReady()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            session.unregister()
        }
    }

    private var commandIntent: Intent? = null
    private var previousApp = UNKNOWN_APP
    private lateinit var gameBar: GameBarService
    private lateinit var gameManager: GameManager

    @SuppressLint("WrongConstant")
    override fun onCreate() {
        isRunning = true
        try {
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        commandIntent = intent
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            START -> onGameStart()
            STOP -> onGameStop()
        }
        return START_STICKY
    }

    private fun onGameStart() {
        Intent(this, GameBarService::class.java).apply {
            bindServiceAsUser(this, gameBarConnection, Context.BIND_AUTO_CREATE, UserHandle.CURRENT)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        // restore settings in case the listener got destroyed gracefully
        session.unregister()
        GameModeUtils.unbind()
        unbindService(gameBarConnection)
        ScreenUtils.unbind(this)
        unregisterReceiver(screenReceiver)
        isRunning = false
        super.onDestroy()
    }

    private fun onGameBarReady() {
        try {
            if (session.state != null) {
                session.unregister()
            }
            val app = commandIntent?.getStringExtra(EXTRA_PACKAGE_NAME) ?: return
            session.register(app)
            applyGameModeConfig(app)
            gameBar.onGameStart()
            ScreenUtils.stayAwake(appSettings.stayAwake)
        } catch (e: Exception) {
            Log.d(TAG, e.toString())
        }
    }

    private fun onGameStop() {
        try {
            if (session.state != null) {
                session.unregister()
            }
            ScreenUtils.stayAwake(false)
            gameBar.onGameLeave()
            stopSelf()
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
        const val TAG = "SessionService"
        const val START = "game_start"
        const val STOP = "game_stop"
        const val EXTRA_PACKAGE_NAME = "package_name"
        const val UNKNOWN_APP = "unknown"
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