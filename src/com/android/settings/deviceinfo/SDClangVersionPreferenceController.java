/*
 * Copyright (C) 2017 Pure Fusion OS
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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceScreen;
import android.text.TextUtils;

import com.android.settings.R;
import com.android.settings.core.PreferenceController;

public class SDClangVersionPreferenceController extends PreferenceController {

    private static final String PROPERTY_MOD_BUILD_COMPILER_SDCLANG = "ro.build.fusion.sdclang";
    private static final String KEY_MOD_BUILD_COMPILER_SDCLANG = "build_compiler_sdclang";

    public SDClangVersionPreferenceController(Context context) {
        super(context);
    }

    @Override
    public boolean isAvailable() {
        return !TextUtils.isEmpty(SystemProperties.get(PROPERTY_MOD_BUILD_COMPILER_SDCLANG));
    }

    @Override
    public String getPreferenceKey() {
        return KEY_MOD_BUILD_COMPILER_SDCLANG;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        final Preference pref = screen.findPreference(KEY_MOD_BUILD_COMPILER_SDCLANG);
        if (pref != null) {
            final String summary = SystemProperties.get(PROPERTY_MOD_BUILD_COMPILER_SDCLANG,
                    mContext.getResources().getString(R.string.build_compiler_default));
            pref.setSummary(summary);
        }
    }
}
