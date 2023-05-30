/*
 * Copyright 2023 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.stanis.apps.fiRun.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.stanis.apps.fiRun.util.settings.Settings
import me.stanis.apps.fiRun.util.settings.Settings.Companion.Key
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings
) : ViewModel() {
    val onlyHighHrAccuracyState = settings.observeBoolean(Key.ONLY_HIGH_HR_ACCURACY)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), false)

    fun setHeartRateAccuracy(state: Boolean) {
        viewModelScope.launch {
            settings.put(Key.ONLY_HIGH_HR_ACCURACY, state)
        }
    }
}
