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

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.view.WindowManager
import com.android.internal.util.ScreenshotHelper
import java.util.function.Consumer

object ScreenUtils {
    @SuppressLint("StaticFieldLeak")  // We store the application context, not an activity.
    var helper: ScreenshotHelper? = null
    fun init(context: Context) {
        if (helper != null) return
        helper = ScreenshotHelper(context.applicationContext)
    }

    fun takeScreenshot(handler: Handler, consumer: Consumer<Uri>? = null) {
        helper?.takeScreenshot(
            WindowManager.TAKE_SCREENSHOT_FULLSCREEN, true, true,
            WindowManager.ScreenshotSource.SCREENSHOT_GLOBAL_ACTIONS, handler, consumer
        )
    }
}
