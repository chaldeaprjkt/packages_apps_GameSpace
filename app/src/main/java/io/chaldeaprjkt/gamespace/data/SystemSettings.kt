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
package io.chaldeaprjkt.gamespace.data

import android.content.Context
import android.provider.Settings
import io.chaldeaprjkt.gamespace.utils.GameModeUtils

class SystemSettings(private val context: Context) {

    private val resolver = context.contentResolver
    private val appSettings by lazy { AppSettings(context) }

    var systemHeadsUp
        get() =
            Settings.Global.getInt(resolver, Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, 1) == 1
        set(it) {
            Settings.Global.putInt(
                resolver,
                Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED,
                it.toInt()
            )
        }

    var autoBrightness
        get() =
            Settings.System.getInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            ) ==
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        set(auto) {
            Settings.System.putInt(
                resolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        }

    var threeScreenshot
        get() = Settings.System.getInt(resolver, Settings.System.THREE_FINGER_GESTURE, 0) == 1
        set(it) {
            Settings.System.putInt(resolver, Settings.System.THREE_FINGER_GESTURE, it.toInt())
        }

    var userGames
        get() =
            Settings.System.getString(resolver, KEY_GAME_LIST)
                ?.split(";")
                ?.toList()?.filter { it.isNotEmpty() }
                ?.map { UserGame.fromSettings(it) } ?: emptyList()
        set(games) {
            Settings.System.putString(
                resolver, KEY_GAME_LIST,
                if (games.isEmpty()) "" else
                    games.joinToString(";") { it.toString() }
            )
            GameModeUtils.setupBatteryMode(context, games.isNotEmpty())
        }

    private fun Boolean.toInt() = if (this) 1 else 0

    fun applyUserSettings(session: SessionState?) {
        session ?: return
        session.headsUp = systemHeadsUp
        session.autoBrightness = autoBrightness
        session.threeScreenshot = threeScreenshot
        if (appSettings.noHeadsUp) {
            systemHeadsUp = false
        }
        if (appSettings.noAutoBrightness) {
            autoBrightness = false
        }
        if (appSettings.noThreeScreenshot) {
            threeScreenshot = false
        }
    }

    fun restoreUserSettings(session: SessionState?) {
        session ?: return
        if (appSettings.noHeadsUp) {
            session.headsUp?.let { systemHeadsUp = it }
        }
        if (appSettings.noAutoBrightness) {
            session.autoBrightness?.let { autoBrightness = it }
        }
        if (appSettings.noThreeScreenshot) {
            session.threeScreenshot?.let { threeScreenshot = it }
        }
    }

    companion object {
        const val KEY_GAME_LIST = "gamespace_game_list"
    }
}
