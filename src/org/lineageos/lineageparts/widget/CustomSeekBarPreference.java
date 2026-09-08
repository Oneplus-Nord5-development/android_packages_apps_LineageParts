/*
 * SPDX-FileCopyrightText: 2026 The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.lineageparts.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceViewHolder;

import com.android.settings.custom.preference.SelfRemovingPreference;

import lineageos.providers.LineageSettings;

import org.lineageos.lineageparts.R;

public class CustomSeekBarPreference extends SelfRemovingPreference
        implements SeekBar.OnSeekBarChangeListener {

    public static final int SETTINGS_TYPE_SYSTEM = 0;
    public static final int SETTINGS_TYPE_SECURE = 1;
    public static final int SETTINGS_TYPE_LINEAGE_SECURE = 2;

    private static final int REPEAT_INTERVAL = 80;

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private Runnable mAutoRepeatRunnable;

    protected int mSettingsType = SETTINGS_TYPE_SYSTEM;
    private int mMin = 0;
    private int mMax = 100;
    private int mDefaultValue = 0;
    private int mInterval = 1;
    private String mUnits = "";
    private boolean mContinuousUpdates = true;

    private int mValue = 0;
    private boolean mTrackingTouch = false;

    private SeekBar mSeekBar;
    private TextView mValueText;
    private ImageView mResetButton;
    private ImageView mMinusButton;
    private ImageView mPlusButton;

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        setLayoutResource(R.layout.preference_custom_seekbar);
        setSelectable(false);
        setPreferenceDataStore(new DataStore());

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.CustomSeekBarPreference,
                defStyleAttr, defStyleRes);
        mMin = a.getInt(R.styleable.CustomSeekBarPreference_min, mMin);
        mMax = a.getInt(R.styleable.CustomSeekBarPreference_max, mMax);
        mInterval = a.getInt(R.styleable.CustomSeekBarPreference_interval, mInterval);
        mUnits = a.getString(R.styleable.CustomSeekBarPreference_units);
        if (mUnits == null) {
            mUnits = "";
        }
        mContinuousUpdates = a.getBoolean(
                R.styleable.CustomSeekBarPreference_continuousUpdates, mContinuousUpdates);
        mSettingsType = a.getInt(
                R.styleable.CustomSeekBarPreference_settingsType, mSettingsType);

        TypedArray aApp = context.obtainStyledAttributes(attrs, new int[] {
                android.R.attr.defaultValue,
                android.R.attr.max
        });
        mDefaultValue = aApp.getInt(0, mMin);
        if (aApp.hasValue(1)) {
            mMax = aApp.getInt(1, mMax);
        }
        aApp.recycle();
        a.recycle();

        if (mInterval <= 0) {
            mInterval = 1;
        }
        if (mMax < mMin) {
            mMax = mMin;
        }
        mValue = mDefaultValue;
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public CustomSeekBarPreference(Context context, AttributeSet attrs) {
        this(context, attrs, androidx.preference.R.attr.preferenceStyle);
    }

    public CustomSeekBarPreference(Context context) {
        this(context, null);
    }

    @Override
    public void onAttached() {
        super.onAttached();
        mValue = getInt(getKey(), mDefaultValue);
        updateViewHolders();
    }

    @Override
    public void onBindViewHolder(@NonNull PreferenceViewHolder holder) {
        super.onBindViewHolder(holder);
        holder.itemView.setClickable(false);

        mSeekBar = (SeekBar) holder.findViewById(R.id.seekbar);
        mValueText = (TextView) holder.findViewById(R.id.value);
        mResetButton = (ImageView) holder.findViewById(R.id.reset_button);
        mMinusButton = (ImageView) holder.findViewById(R.id.minus_button);
        mPlusButton = (ImageView) holder.findViewById(R.id.plus_button);

        mValue = getInt(getKey(), mDefaultValue);

        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(null);
            mSeekBar.setMax(getProgressMax());
            mSeekBar.setProgress(getProgressForValue(mValue));
            mSeekBar.setEnabled(isEnabled());
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        if (mResetButton != null) {
            mResetButton.setOnClickListener(v -> setValue(mDefaultValue));
        }

        if (mValueText != null) {
            mValueText.setOnLongClickListener(v -> {
                setValue(mDefaultValue);
                return true;
            });
        }

        if (mMinusButton != null) {
            setupRepeatButton(mMinusButton, -mInterval);
        }

        if (mPlusButton != null) {
            setupRepeatButton(mPlusButton, mInterval);
        }

        updateViewHolders();
    }

    @Override
    public void onDetached() {
        mHandler.removeCallbacksAndMessages(null);
        mAutoRepeatRunnable = null;
        if (mSeekBar != null) {
            mSeekBar.setOnSeekBarChangeListener(null);
            mSeekBar = null;
        }
        if (mMinusButton != null) {
            mMinusButton.setOnClickListener(null);
            mMinusButton.setOnLongClickListener(null);
            mMinusButton.setOnTouchListener(null);
            mMinusButton = null;
        }
        if (mPlusButton != null) {
            mPlusButton.setOnClickListener(null);
            mPlusButton.setOnLongClickListener(null);
            mPlusButton.setOnTouchListener(null);
            mPlusButton = null;
        }
        if (mResetButton != null) {
            mResetButton.setOnClickListener(null);
            mResetButton = null;
        }
        if (mValueText != null) {
            mValueText.setOnLongClickListener(null);
            mValueText = null;
        }
        super.onDetached();
    }

    private void setupRepeatButton(@NonNull View button, int delta) {
        button.setOnClickListener(v -> changeValue(delta));
        button.setOnLongClickListener(v -> {
            mHandler.removeCallbacksAndMessages(null);
            changeValue(delta);
            mAutoRepeatRunnable = new Runnable() {
                @Override
                public void run() {
                    if (changeValue(delta)) {
                        mHandler.postDelayed(this, REPEAT_INTERVAL);
                    }
                }
            };
            mHandler.postDelayed(mAutoRepeatRunnable, REPEAT_INTERVAL);
            return true;
        });
        button.setOnTouchListener((v, event) -> {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                if (mAutoRepeatRunnable != null) {
                    mHandler.removeCallbacks(mAutoRepeatRunnable);
                    mAutoRepeatRunnable = null;
                }
            }
            return false;
        });
    }

    private boolean changeValue(int delta) {
        int target = mValue + delta;
        target = Math.max(mMin, Math.min(mMax, target));
        if (target != mValue) {
            setValue(target);
            return true;
        }
        return false;
    }

    public void setValue(int value) {
        setValueInternal(value, true);
    }

    public int getValue() {
        return mValue;
    }

    private void setValueInternal(int value, boolean updateViews) {
        value = Math.max(mMin, Math.min(mMax, value));
        mValue = value;
        putInt(getKey(), value);
        callChangeListener(value);
        if (updateViews) {
            updateViewHolders();
        }
    }

    private void updateViewHolders() {
        if (mValueText != null) {
            mValueText.setText(formatValue(mValue));
        }
        if (mSeekBar != null && !mTrackingTouch) {
            int progress = getProgressForValue(mValue);
            if (mSeekBar.getProgress() != progress) {
                mSeekBar.setProgress(progress);
            }
        }
        if (mResetButton != null) {
            mResetButton.setVisibility(mValue != mDefaultValue ? View.VISIBLE : View.INVISIBLE);
        }
    }

    private String formatValue(int value) {
        if (mUnits != null && !mUnits.isEmpty()) {
            return value + " " + mUnits;
        }
        return String.valueOf(value);
    }

    private int getProgressMax() {
        return (mMax - mMin) / mInterval;
    }

    private int getProgressForValue(int value) {
        return (value - mMin) / mInterval;
    }

    private int getValueForProgress(int progress) {
        return mMin + (progress * mInterval);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        if (!fromUser) {
            return;
        }
        int newValue = getValueForProgress(progress);
        newValue = Math.max(mMin, Math.min(mMax, newValue));
        if (mValueText != null) {
            mValueText.setText(formatValue(newValue));
        }
        if (mResetButton != null) {
            mResetButton.setVisibility(newValue != mDefaultValue ? View.VISIBLE : View.INVISIBLE);
        }
        if (mContinuousUpdates) {
            setValueInternal(newValue, false);
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = true;
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        mTrackingTouch = false;
        int newValue = getValueForProgress(seekBar.getProgress());
        setValue(newValue);
    }

    public void setSettingsType(int settingsType) {
        mSettingsType = settingsType;
    }

    public int getSettingsType() {
        return mSettingsType;
    }

    protected boolean isPersisted() {
        if (mSettingsType == SETTINGS_TYPE_LINEAGE_SECURE) {
            return LineageSettings.Secure.getString(getContext().getContentResolver(), getKey()) != null;
        } else if (mSettingsType == SETTINGS_TYPE_SECURE) {
            return Settings.Secure.getString(getContext().getContentResolver(), getKey()) != null;
        }
        return Settings.System.getString(getContext().getContentResolver(), getKey()) != null;
    }

    protected void putInt(String key, int value) {
        if (mSettingsType == SETTINGS_TYPE_LINEAGE_SECURE) {
            LineageSettings.Secure.putInt(getContext().getContentResolver(), key, value);
        } else if (mSettingsType == SETTINGS_TYPE_SECURE) {
            Settings.Secure.putInt(getContext().getContentResolver(), key, value);
        } else {
            Settings.System.putInt(getContext().getContentResolver(), key, value);
        }
    }

    protected int getInt(String key, int defaultValue) {
        if (mSettingsType == SETTINGS_TYPE_LINEAGE_SECURE) {
            return LineageSettings.Secure.getInt(getContext().getContentResolver(), key, defaultValue);
        } else if (mSettingsType == SETTINGS_TYPE_SECURE) {
            return Settings.Secure.getInt(getContext().getContentResolver(), key, defaultValue);
        }
        return Settings.System.getInt(getContext().getContentResolver(), key, defaultValue);
    }

    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, 0);
    }

    @Override
    protected void onSetInitialValue(boolean restorePersistedValue, @Nullable Object defaultValue) {
        int value;
        if (!restorePersistedValue || !isPersisted()) {
            if (defaultValue instanceof Integer) {
                value = (Integer) defaultValue;
            } else if (defaultValue instanceof String) {
                value = Integer.parseInt((String) defaultValue);
            } else {
                value = mDefaultValue;
            }
            putInt(getKey(), value);
        } else {
            value = getInt(getKey(), mDefaultValue);
        }
        setValue(value);
    }

    @Override
    public boolean persistInt(int value) {
        putInt(getKey(), value);
        return true;
    }

    @Override
    public int getPersistedInt(int defaultReturnValue) {
        return getInt(getKey(), defaultReturnValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState myState = new SavedState(superState);
        myState.value = mValue;
        myState.min = mMin;
        myState.max = mMax;
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mMin = myState.min;
        mMax = myState.max;
        setValue(myState.value);
    }

    private static class SavedState extends BaseSavedState {
        int value;
        int min;
        int max;

        SavedState(Parcel source) {
            super(source);
            value = source.readInt();
            min = source.readInt();
            max = source.readInt();
        }

        SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
            dest.writeInt(min);
            dest.writeInt(max);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private class DataStore extends PreferenceDataStore {
        @Override
        public void putInt(String key, int value) {
            CustomSeekBarPreference.this.putInt(key, value);
        }

        @Override
        public int getInt(String key, int defaultValue) {
            return CustomSeekBarPreference.this.getInt(key, defaultValue);
        }
    }
}
