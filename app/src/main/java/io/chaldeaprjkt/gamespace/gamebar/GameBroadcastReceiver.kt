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

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class GameBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            GAME_START -> context.onGameStart(intent)
            GAME_STOP -> context.onGameStop()
        }
    }

    private fun Context.onGameStart(intent: Intent) {
        val app = intent.getStringExtra(SessionService.EXTRA_PACKAGE_NAME)
        SessionService.start(this, app)
    }

    private fun Context.onGameStop() {
        SessionService.stop(this)
    }

    companion object {
        const val GAME_START = "io.chaldeaprjkt.gamespace.action.GAME_START"
        const val GAME_STOP = "io.chaldeaprjkt.gamespace.action.GAME_STOP"
    }
}

