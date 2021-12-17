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
import android.content.pm.PackageManager
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import io.chaldeaprjkt.gamespace.R

class AppsAdapter(private val pm: PackageManager, private val apps: List<ApplicationInfo>) :
    ListAdapter<ApplicationInfo, AppsItemViewHolder>(DiffCallback(pm)) {

    private lateinit var onClick: (ApplicationInfo) -> Unit

    init {
        submitList(apps)
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppsItemViewHolder {
        return AppsItemViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.app_selector_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: AppsItemViewHolder, position: Int) {
        holder.bind(currentList[position]) {
            if (::onClick.isInitialized) {
                onClick.invoke(it)
            }
        }
    }

    fun onItemClick(action: (ApplicationInfo) -> Unit) {
        onClick = action
    }

    fun filterWith(text: String?) {
        val rText = ".*${text}.*".toRegex(RegexOption.IGNORE_CASE)
        apps.filter { it.loadLabel(pm).contains(rText) }
            .takeIf { it.isNotEmpty() }
            ?.run(::submitList) ?: submitList(apps)
    }

    private class DiffCallback(private val pm: PackageManager) :
        DiffUtil.ItemCallback<ApplicationInfo>() {
        override fun areItemsTheSame(oldItem: ApplicationInfo, newItem: ApplicationInfo) =
            oldItem.loadLabel(pm) == newItem.loadLabel(pm)

        override fun areContentsTheSame(oldItem: ApplicationInfo, newItem: ApplicationInfo) =
            oldItem.packageName == newItem.packageName
    }
}
