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
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import android.widget.SearchView
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.preferences.AppListPreferences
import com.android.settingslib.R as SettingsR
import com.google.android.material.appbar.AppBarLayout

class AppSelectorFragment : Fragment(), SearchView.OnQueryTextListener,
    MenuItem.OnActionExpandListener {
    private var appListView: RecyclerView? = null
    private var settings: SystemSettings? = null
    private var appListAdapter: AppListAdapter? = null
    private var appBarLayout: AppBarLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        super.onCreateView(inflater, container, savedInstanceState)
        appBarLayout = activity?.findViewById(SettingsR.id.app_bar)
        return inflater.inflate(R.layout.app_selector, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        settings = context?.let { SystemSettings(it) }
        view.findViewById<RecyclerView>(R.id.app_list)?.apply {
            setupAppListView(this)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.app_selector_menu, menu)
        val searchMenuItem = menu.findItem(R.id.app_search_menu)
        val searchView = searchMenuItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.queryHint = getString(R.string.app_search_title)
        searchMenuItem.setOnActionExpandListener(this)
    }

    private fun setupAppListView(view: RecyclerView) {
        appListView = view
        val apps = view.context.packageManager
            .getInstalledApplications(PackageManager.GET_META_DATA)
            .filter {
                it.packageName != context?.packageName &&
                        it.flags and ApplicationInfo.FLAG_SYSTEM == 0 &&
                        settings?.userGames?.contains(it.packageName) == false
            }
            .sortedBy { it.loadLabel(view.context.packageManager).toString().lowercase() }

        appListAdapter = AppListAdapter(apps)
        view.adapter = appListAdapter
        view.layoutManager = LinearLayoutManager(view.context)
        appListAdapter?.onItemClick {
            activity?.setResult(Activity.RESULT_OK, Intent().apply {
                putExtra(AppListPreferences.EXTRA_APP, it.packageName)
            })
            activity?.finish()
        }
    }

    class AppListAdapter(private val apps: List<ApplicationInfo>) :
        RecyclerView.Adapter<AppListAdapter.AppViewHolder>() {

        private lateinit var onClick: (ApplicationInfo) -> Unit
        private val filteredList = mutableListOf<ApplicationInfo>()

        class AppViewHolder(private val v: View) : RecyclerView.ViewHolder(v) {
            private val pm by lazy { v.context.packageManager }

            fun bind(app: ApplicationInfo) {
                v.findViewById<TextView>(R.id.app_name)?.text = app.loadLabel(pm)
                v.findViewById<TextView>(R.id.app_summary)?.text = app.packageName
                v.findViewById<ImageView>(R.id.app_icon)?.setImageDrawable(app.loadIcon(pm))
            }
        }

        override fun getItemCount(): Int {
            return if (filteredList.isEmpty()) apps.size else filteredList.size
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            return AppViewHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.app_selector_item, parent, false)
            )
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
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

    override fun onQueryTextSubmit(query: String?) = false

    override fun onQueryTextChange(newText: String?): Boolean {
        appListAdapter?.filterWith(context, newText)
        return false
    }

    override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
        appBarLayout?.setExpanded(false, false)
        appListView?.let { ViewCompat.setNestedScrollingEnabled(it, false) }
        return true
    }

    override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
        appBarLayout?.setExpanded(false, false)
        appListView?.let { ViewCompat.setNestedScrollingEnabled(it, true) }
        return true
    }
}
