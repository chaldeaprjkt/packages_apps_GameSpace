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
package io.chaldeaprjkt.gamespace.widget.tiles

import android.app.GameManager
import android.content.Context
import android.util.AttributeSet
import android.view.View
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.utils.GameModeUtils.Companion.describeGameMode
import io.chaldeaprjkt.gamespace.utils.di.ServiceViewEntryPoint
import io.chaldeaprjkt.gamespace.utils.entryPointOf

class GameModeTile @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : BaseTile(context, attrs) {

    private val gameModeUtils by lazy {
        context.entryPointOf<ServiceViewEntryPoint>().gameModeUtils()
    }

    private val modes = listOf(
        GameManager.GAME_MODE_STANDARD,
        GameManager.GAME_MODE_PERFORMANCE,
        GameManager.GAME_MODE_BATTERY,
    )

    private var activeMode = GameManager.GAME_MODE_STANDARD
        set(value) {
            field = value
            summary?.text = context.describeGameMode(value)
            isSelected = value != GameManager.GAME_MODE_STANDARD
            gameModeUtils.setActiveGameMode(systemSettings, value)
        }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        title?.text = context.getString(R.string.game_mode_title)
        activeMode = gameModeUtils.activeGame?.mode ?: GameManager.GAME_MODE_STANDARD
        icon?.setImageResource(R.drawable.ic_speed)
    }

    override fun onClick(v: View?) {
        super.onClick(v)
        val current = modes.indexOf(activeMode)
        activeMode = modes[if (current == modes.size - 1) 0 else current + 1]
    }
}