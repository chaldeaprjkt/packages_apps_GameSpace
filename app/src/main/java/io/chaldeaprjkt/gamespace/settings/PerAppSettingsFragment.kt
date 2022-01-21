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
package io.chaldeaprjkt.gamespace.settings

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.ListPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import dagger.hilt.android.AndroidEntryPoint
import io.chaldeaprjkt.gamespace.R
import io.chaldeaprjkt.gamespace.data.GameConfig
import io.chaldeaprjkt.gamespace.data.SystemSettings
import io.chaldeaprjkt.gamespace.data.UserGame
import io.chaldeaprjkt.gamespace.utils.GameModeUtils
import javax.inject.Inject

@AndroidEntryPoint(PreferenceFragmentCompat::class)
class PerAppSettingsFragment : Hilt_PerAppSettingsFragment(),
    Preference.OnPreferenceChangeListener {

    @Inject
    lateinit var settings: SystemSettings

    @Inject
    lateinit var gameModeUtils: GameModeUtils

    private val currentGame by lazy {
        context?.packageManager?.getApplicationInfo(
            activity?.intent?.getStringExtra(PerAppSettingsActivity.EXTRA_PACKAGE),
            0
        )
    }

    private val currentConfig: UserGame?
        get() = settings.userGames.firstOrNull { it.packageName == currentGame?.packageName }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        activity?.title = context?.getString(R.string.per_app_title)
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.per_app_preferences, rootKey)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        findPreference<Preference>("headers")?.apply {
            layoutResource = R.layout.per_app_header
            icon = currentGame?.loadIcon(context?.packageManager)
            title = context?.packageManager?.let { currentGame?.loadLabel(it) }
        }
        findPreference<ListPreference>(PREF_PREFERRED_MODE)?.apply {
            currentConfig?.mode?.let { value = it.toString() }
            onPreferenceChangeListener = this@PerAppSettingsFragment
        }
        findPreference<SwitchPreference>(PREF_USE_ANGLE)?.apply {
            context?.resources?.getBoolean(R.bool.config_allow_per_app_angle_usage)?.let {
                isVisible = it
                if (!it) return@apply
            }

            if (gameModeUtils.findAnglePackage()?.isEnabled != true) {
                isEnabled = false
                summary = context.getString(R.string.cant_find_angle_pkg)
                return@apply
            }
            isChecked = gameModeUtils.isAngleUsed(currentGame?.packageName)
            onPreferenceChangeListener = this@PerAppSettingsFragment

        }
        findPreference<Preference>(PREF_UNREGISTER)?.apply {
            summary = context.getString(
                R.string.per_app_unregister,
                currentGame?.loadLabel(context?.packageManager)
            )
            setOnPreferenceClickListener {
                activity?.setResult(Activity.RESULT_OK, Intent().apply {
                    putExtra(PREF_UNREGISTER, currentGame?.packageName)
                })
                activity?.finish()
                true
            }
        }
    }

    override fun onPreferenceChange(preference: Preference?, newValue: Any?): Boolean {
        val gameInfo = currentGame ?: return false
        when (preference?.key) {
            PREF_PREFERRED_MODE -> {
                val newMode = (newValue as String).toIntOrNull() ?: 1
                gameModeUtils.setGameModeFor(gameInfo.packageName, settings, newMode)
                return true
            }
            PREF_USE_ANGLE -> {
                val newModes = GameConfig.ModeBuilder.apply {
                    useAngle = newValue as Boolean
                }.build()
                gameModeUtils.setIntervention(gameInfo.packageName, newModes)
                return true
            }
        }
        return false
    }

    companion object {
        const val PREF_PREFERRED_MODE = "per_app_preferred_mode"
        const val PREF_USE_ANGLE = "per_app_use_angle"
        const val PREF_UNREGISTER = "per_app_unregister"
    }
}
