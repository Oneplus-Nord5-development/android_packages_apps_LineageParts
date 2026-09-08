/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.widget;

import android.content.Context;
import android.util.AttributeSet;

public class LineageSecureSettingCustomSeekBarPreference extends CustomSeekBarPreference {

    public LineageSecureSettingCustomSeekBarPreference(Context context, AttributeSet attrs,
            int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setSettingsType(SETTINGS_TYPE_LINEAGE_SECURE);
    }

    public LineageSecureSettingCustomSeekBarPreference(Context context, AttributeSet attrs,
            int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setSettingsType(SETTINGS_TYPE_LINEAGE_SECURE);
    }

    public LineageSecureSettingCustomSeekBarPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setSettingsType(SETTINGS_TYPE_LINEAGE_SECURE);
    }

    public LineageSecureSettingCustomSeekBarPreference(Context context) {
        super(context);
        setSettingsType(SETTINGS_TYPE_LINEAGE_SECURE);
    }
}
