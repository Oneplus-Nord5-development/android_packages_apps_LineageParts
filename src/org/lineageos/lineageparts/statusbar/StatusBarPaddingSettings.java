/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.statusbar;

import android.os.Bundle;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import org.lineageos.lineageparts.R;
import org.lineageos.lineageparts.SettingsPreferenceFragment;
import org.lineageos.lineageparts.widget.CustomSeekBarPreference;

public class StatusBarPaddingSettings extends SettingsPreferenceFragment {

    private static final String KEY_PADDING_LEFT = "status_bar_padding_left";
    private static final String KEY_PADDING_RIGHT = "status_bar_padding_right";
    private static final String KEY_PADDING_TOP = "status_bar_padding_top";
    private static final String KEY_PADDING_BOTTOM = "status_bar_padding_bottom";
    private static final int MENU_RESET = Menu.FIRST;

    private CustomSeekBarPreference mPaddingLeft;
    private CustomSeekBarPreference mPaddingRight;
    private CustomSeekBarPreference mPaddingTop;
    private CustomSeekBarPreference mPaddingBottom;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.status_bar_padding_settings);
        getActivity().setTitle(R.string.status_bar_padding_title);

        mPaddingLeft = findPreference(KEY_PADDING_LEFT);
        mPaddingRight = findPreference(KEY_PADDING_RIGHT);
        mPaddingTop = findPreference(KEY_PADDING_TOP);
        mPaddingBottom = findPreference(KEY_PADDING_BOTTOM);

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.reset)
                .setIcon(R.drawable.ic_settings_backup_restore)
                .setAlphabeticShortcut('r')
                .setShowAsActionFlags(
                        MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_RESET) {
            resetToDefaults();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void resetToDefaults() {
        if (mPaddingLeft != null) mPaddingLeft.setValue(0);
        if (mPaddingRight != null) mPaddingRight.setValue(0);
        if (mPaddingTop != null) mPaddingTop.setValue(0);
        if (mPaddingBottom != null) mPaddingBottom.setValue(0);
    }
}
