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

import android.media.AudioManager
import androidx.annotation.Keep

@Keep
data class SessionState(
    var packageName: String,
    var autoBrightness: Boolean? = null,
    var headsUp: Boolean? = null,
    var threeScreenshot: Boolean? = null,
    var ringerMode: Int = AudioManager.RINGER_MODE_NORMAL,
)
