/*
 * SPDX-FileCopyrightText: 2024 LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.spoofing

import android.os.Bundle
import android.provider.Settings
import androidx.preference.SwitchPreferenceCompat
import org.lineageos.lineageparts.R
import org.lineageos.lineageparts.SettingsPreferenceFragment

class Spoofing : SettingsPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.spoofing)

        val hideMockLocation = findPreference<SwitchPreferenceCompat>("hide_mock_location")
        hideMockLocation?.apply {
            isChecked = Settings.System.getInt(requireContext().contentResolver,
                "hide_mock_location", 0) == 1
            setOnPreferenceChangeListener { _, newValue ->
                Settings.System.putInt(requireContext().contentResolver,
                    "hide_mock_location", if (newValue as Boolean) 1 else 0)
                true
            }
        }
    }
}
