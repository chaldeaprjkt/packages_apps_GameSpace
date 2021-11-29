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
package io.chaldeaprjkt.gamespace.preferences.appselector

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.DeviceSettings
import io.chaldeaprjkt.gamespace.preferences.AppListPreferences


class AppSelectorFragment : Fragment() {
    private var settings: DeviceSettings? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        return inflater.inflate(R.layout.app_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = context?.let { DeviceSettings(it) }
        view.findViewById<RecyclerView>(R.id.app_list)?.apply {
            setupAppListView(this)
        }
    }

    private fun setupAppListView(view: RecyclerView) {
        val apps = view.context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                it.packageName != context?.packageName &&
                        it.flags and ApplicationInfo.FLAG_SYSTEM == 0 &&
                        settings?.userGames?.contains(it.packageName) == false
            }
            .sortedBy { it.loadLabel(view.context.packageManager).toString().lowercase() }


        view.adapter = AppListAdapter(apps)
        view.layoutManager = LinearLayoutManager(view.context)
        (view.adapter as AppListAdapter).onItemClick {
            activity?.setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(AppListPreferences.EXTRA_APP, it.packageName)
            })
            activity?.onBackPressed()
        }
    }

    class AppListAdapter(private val apps: List<ApplicationInfo>) :
        RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

        private lateinit var onClick: (ApplicationInfo) -> Unit

        class AppViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
            private val pm by lazy { v.context.packageManager }

            fun bind(app: ApplicationInfo) {
                v.findViewById<TextView>(R.id.app_name)?.text = app.loadLabel(pm)
                v.findViewById<TextView>(R.id.app_summary)?.text = app.packageName
                v.findViewById<ImageView>(R.id.app_icon)?.setImageDrawable(app.loadIcon(pm))
            }
        }

        override fun getItemCount(): Int {
            return apps.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            return AppViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.app_selector_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            holder.bind(apps[position])
            holder.itemView.setOnClickListener {
                if (::onClick.isInitialized) {
                    onClick.invoke(apps[position])
                }
            }
        }

        fun onItemClick(action: (ApplicationInfo) -> Unit) {
            onClick = action
        }
    }

}
