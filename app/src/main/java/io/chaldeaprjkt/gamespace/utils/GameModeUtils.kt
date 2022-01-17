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

import android.app.GameManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IDeviceIdleController
import android.os.RemoteException
import android.os.ServiceManager
import android.provider.DeviceConfig
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.GameConfig
import io.chaldeaprjkt.gamespace.data.GameConfig.Companion.asConfig
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.data.UserGame
import javax.inject.Inject

class GameModeUtils @Inject constructor(private val context: Context) {

    private val defaultModes = listOf(
        GameConfig(GameManager.GAME_MODE_PERFORMANCE, downscaleFactor = .7f),
        GameConfig(GameManager.GAME_MODE_BATTERY, downscaleFactor = .8f)
    )
    private var manager: GameManager? = null
    var activeGame: UserGame? = null

    fun bind(manager: GameManager) {
        this.manager = manager
    }

    fun unbind() {
        manager = null
    }

    fun setIntervention(packageName: String, modeData: List<GameConfig>? = defaultModes) {
        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_GAME_OVERLAY, packageName, modeData?.asConfig(), false
        )
    }

    fun setActiveGameMode(systemSettings: SystemSettings, mode: Int) {
        val packageName = activeGame?.packageName ?: return
        manager?.setGameMode(packageName, mode)
        activeGame = setGameModeFor(packageName, systemSettings, mode)
    }

    fun setGameModeFor(packageName: String, systemSettings: SystemSettings, mode: Int): UserGame {
        val data = UserGame(packageName, mode)
        systemSettings.userGames = systemSettings.userGames
            .filter { x -> x.packageName != packageName }
            .toMutableList()
            .apply { add(data) }

        return data
    }

    fun setupBatteryMode(enable: Boolean) {
        val svc = IDeviceIdleController.Stub.asInterface(
            ServiceManager.getService(Context.DEVICE_IDLE_CONTROLLER)
        )
        try {
            val isListed = svc?.isPowerSaveWhitelistApp(context.packageName) ?: false
            if (enable && !isListed) {
                svc?.addPowerSaveWhitelistApp(context.packageName)
            } else if (!enable && isListed) {
                svc?.removePowerSaveWhitelistApp(context.packageName)
            }
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }

    companion object {
        const val defaultPreferredMode = GameManager.GAME_MODE_STANDARD
        fun Context.describeGameMode(mode: Int) =
            resources.getStringArray(R.array.game_mode_names)[mode] ?: "Unsupported"
    }
}
