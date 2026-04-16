/*
 * SPDX-FileCopyrightText: 2024 LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.spoofing

import android.os.Bundle
import org.lineageos.lineageparts.R
import org.lineageos.lineageparts.SettingsPreferenceFragment

class Spoofing : SettingsPreferenceFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.spoofing)
    }
}
