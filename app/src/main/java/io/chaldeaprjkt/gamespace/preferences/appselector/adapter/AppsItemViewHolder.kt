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
package io.chaldeaprjkt.gamespace.preferences.appselector.adapter

import android.content.pm.ApplicationInfo
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import io.chaldeaprjkt.gamespace.R

class AppsItemViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
    private val pm by lazy { v.context.packageManager }

    fun bind(app: ApplicationInfo, onClick: (ApplicationInfo) -> Unit) {
        v.findViewById<TextView>(R.id.app_name)?.text = app.loadLabel(pm)
        v.findViewById<TextView>(R.id.app_summary)?.text = app.packageName
        v.findViewById<ImageView>(R.id.app_icon)?.setImageDrawable(app.loadIcon(pm))
        v.findViewById<ViewGroup>(R.id.app_item)?.setOnClickListener {
            onClick.invoke(app)
        }
    }
}
