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

class DeviceSettings(private val context: Context) {

    var systemHeadsUp
        get() =
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, 1
            ) == 1
        set(enabled) {
            Settings.Global.putInt(
                context.contentResolver,
                Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED, if (enabled) 1 else 0
            )
        }

    var autoBrightness
        get() =
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
            ) ==
                    Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        set(auto) {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                if (auto) Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
                else Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
        }

    var userGames
        get() =
            Settings.System.getString(context.contentResolver, KEY_GAME_LIST)
                ?.split(";")
                ?.toList()?.filter { it.isNotEmpty() } ?: emptyList()
        set(games) {
            Settings.System.putString(
                context.contentResolver,
                KEY_GAME_LIST,
                if (games.isEmpty()) "" else
                    games.joinToString(";")
            )
        }

    var userNoHeadsUp
        get() = Settings.System.getInt(context.contentResolver, KEY_HEADS_UP_DISABLE, 0) == 1
        set(value) {
            Settings.System.putInt(
                context.contentResolver, KEY_HEADS_UP_DISABLE, if (value) 1 else 0
            )
        }
    var userNoAutoBrightness
        get() = Settings.System.getInt(context.contentResolver, KEY_AUTO_BRIGHTNESS_DISABLE, 0) == 1
        set(value) {
            Settings.System.putInt(
                context.contentResolver, KEY_AUTO_BRIGHTNESS_DISABLE, if (value) 1 else 0
            )
        }

    companion object {
        const val KEY_GAME_LIST = "gamespace_game_list"
        const val KEY_HEADS_UP_DISABLE = "gamespace_heads_up_disabled"
        const val KEY_AUTO_BRIGHTNESS_DISABLE = "gamespace_auto_brightness_disabled"
    }
}
