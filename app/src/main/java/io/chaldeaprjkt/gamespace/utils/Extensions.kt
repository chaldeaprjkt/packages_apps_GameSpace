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

import android.app.Activity
import android.content.Context
import android.content.res.Resources.getSystem
import android.graphics.Point
import android.view.View
import android.view.WindowManager
import dagger.hilt.EntryPoints
import io.chaldeaprjkt.gamespace.gamebar.DraggableTouchListener

fun View.registerDraggableTouchListener(
    initPoint: () -> Point,
    listener: (x: Int, y: Int) -> Unit,
    onComplete: () -> Unit
) = DraggableTouchListener(context, this, initPoint, listener, onComplete)

val Context.statusbarHeight
    get() =
        resources.getIdentifier("status_bar_height", "dimen", "android")
            .takeIf { it > 0 }
            ?.let { resources.getDimensionPixelSize(it) } ?: 24.dp

val Int.dp
    get() = (this * getSystem().displayMetrics.density).toInt()

fun WindowManager.isPortrait() =
    maximumWindowMetrics.bounds.width() < maximumWindowMetrics.bounds.height()

fun Activity.assertStarterOrigin() =
    intent?.getStringExtra("referer")?.takeIf { it.isNotEmpty() }
        ?: throw SecurityException("failed to assert starter origin")

inline fun <reified T : Any> Context.entryPointOf(): T =
    EntryPoints.get(applicationContext, T::class.java)
