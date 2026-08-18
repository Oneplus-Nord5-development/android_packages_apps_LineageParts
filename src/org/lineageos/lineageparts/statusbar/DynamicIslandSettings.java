/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.lineageos.lineageparts.statusbar;

import android.os.Bundle;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;

public class DynamicIslandSettings extends SettingsPreferenceFragment {

    private static final String KEY_PINNED_SPORTS = "status_bar_dynamic_island_pinned_sports";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.dynamic_island_settings);
        getActivity().setTitle(R.string.status_bar_dynamic_island_title);
    }

    @Override
    public boolean onPreferenceTreeClick(androidx.preference.Preference preference) {
        if (KEY_PINNED_SPORTS.equals(preference.getKey())) {
            android.content.Intent intent = new android.content.Intent();
            intent.setComponent(new android.content.ComponentName(
                    "org.lineageos.sportsfetcher", "org.lineageos.sportsfetcher.MainActivity"));
            intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                startActivity(intent);
                return true;
            } catch (android.content.ActivityNotFoundException e) {
                // Fallback to default
            }
        }
        return super.onPreferenceTreeClick(preference);
    }
}