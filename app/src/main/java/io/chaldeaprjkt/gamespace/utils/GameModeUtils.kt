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
import android.provider.DeviceConfig
import io.chaldeaprjkt.gamespace.data.GameConfig
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.data.UserGame
import io.chaldeaprjkt.gamespace.data.asConfig

object GameModeUtils {
    const val defaultPreferredMode = GameManager.GAME_MODE_PERFORMANCE

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

    fun setupIntervention(packageName: String, modeData: List<GameConfig> = defaultModes) {
        DeviceConfig.setProperty(
            DeviceConfig.NAMESPACE_GAME_OVERLAY, packageName,
            modeData.asConfig(), false
        )
    }

    fun clearIntervention(packageName: String) {
        // Since we have no api for removing DC, let's use shell for now
        val cmd = "device_config delete ${DeviceConfig.NAMESPACE_GAME_OVERLAY} $packageName"
        try {
            Runtime.getRuntime().exec(cmd)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setActiveGameMode(context: Context, mode: Int) {
        val packageName = activeGame?.packageName ?: return
        manager?.setGameMode(packageName, mode)
        UserGame(packageName, mode).let {
            updatePreferredSettings(context, it)
            activeGame = it
        }
    }

    private fun updatePreferredSettings(context: Context, game: UserGame) {
        SystemSettings(context).let { sys ->
            sys.userGames = sys.userGames.filter { it.packageName != game.packageName }
                .toMutableList()
                .apply { add(game) }
        }
    }
}
