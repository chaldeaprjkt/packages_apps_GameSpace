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

import android.app.GameManager

/**
 * data class for setting up the Game Mode API Intervention
 */
data class GameConfig(val mode: Int, val downscaleFactor: Float, val useAngle: Boolean = false) {
    override fun toString(): String =
        hashMapOf<String, Any>().apply {
            put("mode", mode)
            put("downscaleFactor", "%.1f".format(downscaleFactor))
            // intentionally optional as game may already using it by default
            if (useAngle) put("useAngle", true)
        }.map { (k, v) -> "$k=$v" }.joinToString(",")

    companion object {
        fun Iterable<GameConfig>.asConfig() = this.joinToString(":") { it.toString() }
    }

    object ModeBuilder {
        var useAngle = false

        fun build() = listOf(
            GameConfig(GameManager.GAME_MODE_PERFORMANCE, .7f, useAngle),
            GameConfig(GameManager.GAME_MODE_BATTERY, .8f, useAngle)
        )
    }
}
