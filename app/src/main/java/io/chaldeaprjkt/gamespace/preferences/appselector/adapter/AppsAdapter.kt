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

import android.content.Context
import android.content.pm.ApplicationInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.chaldeaprjkt.gamespace.R

class AppsAdapter(private val apps: List<ApplicationInfo>) :
    RecyclerView.Adapter<AppsItemViewHolder>() {

    private lateinit var onClick: (ApplicationInfo) -> Unit
    private val filteredList = mutableListOf<ApplicationInfo>()

    override fun getItemCount(): Int {
        return if (filteredList.isEmpty()) apps.size else filteredList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsItemViewHolder {
        return AppsItemViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.app_selector_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AppsItemViewHolder, position: Int) {
        val displayApp = if (filteredList.isEmpty()) apps else filteredList
        holder.bind(displayApp[position])
        holder.itemView.setOnClickListener {
            if (::onClick.isInitialized) {
                onClick.invoke(displayApp[position])
            }
        }
    }

    fun onItemClick(action: (ApplicationInfo) -> Unit) {
        onClick = action
    }

    fun filterWith(context: Context?, text: String?) {
        val pm = context?.packageManager ?: return
        val rText = ".*${text}.*".toRegex(RegexOption.IGNORE_CASE)
        apps.filter { it.loadLabel(pm).contains(rText) }
            .takeIf { it.isNotEmpty() }
            ?.let {
                filteredList.clear()
                filteredList.addAll(it)
                notifyDataSetChanged()
            } ?: let {
            filteredList.clear()
            notifyDataSetChanged()
        }
    }
}
