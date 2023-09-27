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

package me.stanis.apps.fiRun.ui.devices

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import me.stanis.apps.fiRun.models.HrDeviceInfo
import me.stanis.apps.fiRun.persistence.DeviceManager
import me.stanis.apps.fiRun.ui.BaseViewModel
import me.stanis.apps.fiRun.ui.devices.model.UiState

@HiltViewModel
class DevicesViewModel @Inject constructor(
    private val deviceManager: DeviceManager
) : BaseViewModel() {

    val uiStateFlow = combine(
        deviceManager.knownDevices,
        deviceManager.searchState
    ) { knownDevices, searchState ->
        UiState(
            knownDevices = knownDevices,
            searchState = searchState
        )
    }.asState(UiState.INITIAL)

    fun connectDevice(identifier: String) {
        viewModelScope.launch {
            deviceManager.connectDevice(identifier)
        }
    }

    fun startSearch() {
        viewModelScope.launch {
            deviceManager.startSearch(true)
        }
    }

    fun stopSearch() {
        viewModelScope.launch {
            deviceManager.stopSearch()
        }
    }

    fun saveDevice(device: HrDeviceInfo) {
        viewModelScope.launch {
            deviceManager.saveDevice(device)
        }
    }

    fun disconnectDevice(identifier: String) {
        viewModelScope.launch {
            deviceManager.disconnectDevice(identifier)
        }
    }
}
