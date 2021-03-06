/*
 * Copyright (C) 2017 The Android Open Source Project
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

package com.android.settings.wifi;

import static android.provider.Settings.Global.USE_OPEN_WIFI_PACKAGE;
import static com.android.settings.wifi.UseOpenWifiPreferenceController.REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY;
import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkScoreManager;
import android.net.NetworkScorerAppData;
import android.provider.Settings;
import android.support.v14.preference.SwitchPreference;
import android.support.v7.preference.Preference;

import com.android.settings.network.NetworkScoreManagerWrapper;
import com.android.settings.SettingsRobolectricTestRunner;
import com.android.settings.TestConfig;
import com.android.settings.core.lifecycle.Lifecycle;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class UseOpenWifiPreferenceControllerTest {
    private static ComponentName ENABLE_ACTIVITY_COMPONENT = new ComponentName("package", "activityClass");
    private static NetworkScorerAppData APP_DATA =
            new NetworkScorerAppData(0, null, null, ENABLE_ACTIVITY_COMPONENT, null);
    private static NetworkScorerAppData APP_DATA_NO_ACTIVITY =
            new NetworkScorerAppData(0, null, null, null, null);

    @Mock private Lifecycle mLifecycle;
    @Mock private Fragment mFragment;
    @Mock private NetworkScoreManagerWrapper mNetworkScoreManagerWrapper;
    @Captor private ArgumentCaptor<Intent> mIntentCaptor;
    private Context mContext;
    private UseOpenWifiPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        mContext = RuntimeEnvironment.application;
    }

    private void createController() {
        mController = new UseOpenWifiPreferenceController(
                mContext, mFragment, mNetworkScoreManagerWrapper, mLifecycle);
    }

    @Test
    public void testIsAvailable_noScorer() {
        when(mNetworkScoreManagerWrapper.getActiveScorer()).thenReturn(null);

        createController();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_noEnableActivity() {
        when(mNetworkScoreManagerWrapper.getActiveScorer()).thenReturn(APP_DATA_NO_ACTIVITY);

        createController();

        assertThat(mController.isAvailable()).isFalse();
    }

    @Test
    public void testIsAvailable_enableActivityExists() {
        when(mNetworkScoreManagerWrapper.getActiveScorer()).thenReturn(APP_DATA);

        createController();

        assertThat(mController.isAvailable()).isTrue();
    }

    @Test
    public void onPreferenceChange_nonMatchingKey_shouldDoNothing() {
        createController();

        final SwitchPreference pref = new SwitchPreference(mContext);

        assertThat(mController.onPreferenceChange(pref, null)).isFalse();
    }

    @Test
    public void onPreferenceChange_notAvailable_shouldDoNothing() {
        when(mNetworkScoreManagerWrapper.getActiveScorer()).thenReturn(APP_DATA_NO_ACTIVITY);

        createController();

        final Preference pref = new Preference(mContext);
        pref.setKey(mController.getPreferenceKey());

        assertThat(mController.onPreferenceChange(pref, null)).isFalse();
    }

    @Test
    public void onPreferenceChange_matchingKeyAndAvailable_enableShouldStartEnableActivity() {
        when(mNetworkScoreManagerWrapper.getActiveScorer()).thenReturn(APP_DATA);
        createController();

        final SwitchPreference pref = new SwitchPreference(mContext);
        pref.setKey(mController.getPreferenceKey());

        assertThat(mController.onPreferenceChange(pref, null)).isFalse();
        verify(mFragment).startActivityForResult(mIntentCaptor.capture(),
                eq(REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY));
        Intent activityIntent = mIntentCaptor.getValue();
        assertThat(activityIntent.getComponent()).isEqualTo(ENABLE_ACTIVITY_COMPONENT);
        assertThat(activityIntent.getAction()).isEqualTo(NetworkScoreManager.ACTION_CUSTOM_ENABLE);
    }

    @Test
    public void onPreferenceChange_matchingKeyAndAvailable_disableShouldUpdateSetting() {
        when(mNetworkScoreManagerWrapper.getActiveScorer()).thenReturn(APP_DATA);
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE,
                ENABLE_ACTIVITY_COMPONENT.getPackageName());

        createController();

        final SwitchPreference pref = new SwitchPreference(mContext);
        pref.setKey(mController.getPreferenceKey());

        assertThat(mController.onPreferenceChange(pref, null)).isTrue();
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isEqualTo("");
    }

    @Test
    public void onActivityResult_nonmatchingRequestCode_shouldDoNothing() {
        createController();

        assertThat(mController.onActivityResult(234 /* requestCode */ , Activity.RESULT_OK))
                .isEqualTo(false);
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isNull();
    }

    @Test
    public void onActivityResult_matchingRequestCode_nonOkResult_shouldDoNothing() {
        createController();

        assertThat(mController
                .onActivityResult(REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY, Activity.RESULT_CANCELED))
                .isEqualTo(true);
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isNull();
    }

    @Test
    public void onActivityResult_matchingRequestCode_okResult_updatesSetting() {
        when(mNetworkScoreManagerWrapper.getActiveScorer()).thenReturn(APP_DATA);
        createController();

        assertThat(mController
                .onActivityResult(REQUEST_CODE_OPEN_WIFI_AUTOMATICALLY, Activity.RESULT_OK))
                .isEqualTo(true);
        assertThat(Settings.Global.getString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE))
                .isEqualTo(ENABLE_ACTIVITY_COMPONENT.getPackageName());
    }

    @Test
    public void updateState_preferenceSetCheckedAndSetVisibleWhenSettingsAreEnabled() {
        when(mNetworkScoreManagerWrapper.getActiveScorer()).thenReturn(APP_DATA);
        createController();

        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE,
                ENABLE_ACTIVITY_COMPONENT.getPackageName());

        mController.updateState(preference);

        verify(preference).setVisible(true);
        verify(preference).setChecked(true);
    }

    @Test
    public void updateState_preferenceSetCheckedAndSetVisibleWhenSettingsAreDisabled() {
        final SwitchPreference preference = mock(SwitchPreference.class);
        Settings.Global.putString(mContext.getContentResolver(), USE_OPEN_WIFI_PACKAGE, "");
        createController();

        mController.updateState(preference);

        verify(preference).setVisible(false);
        verify(preference).setChecked(false);
    }
}
