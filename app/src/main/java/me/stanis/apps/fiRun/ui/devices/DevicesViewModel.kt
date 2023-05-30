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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.stanis.apps.fiRun.services.BoundServiceRepository
import me.stanis.apps.fiRun.services.polar.DeviceConnectionStatus
import me.stanis.apps.fiRun.services.polar.PolarBinder
import me.stanis.apps.fiRun.services.polar.PolarService
import me.stanis.apps.fiRun.services.polar.SearchStatus
import me.stanis.apps.fiRun.util.settings.Settings
import me.stanis.apps.fiRun.util.settings.Settings.Companion.Key
import javax.inject.Inject

@HiltViewModel
class DevicesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings
) : ViewModel() {
    private val polarBinder = BoundServiceRepository<PolarBinder>(context)
    val searchStatus
        get() = polarBinder.flowWhenConnected { searchStatus }
            .stateIn(viewModelScope, SharingStarted.Eagerly, SearchStatus.NoSearch)

    val deviceStatus
        get() = polarBinder.flowWhenConnected { deviceStatus }
            .stateIn(viewModelScope, SharingStarted.Eagerly, DeviceConnectionStatus.NotConnected)

    val devices = polarBinder.flowWhenConnected { devicesFound }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())

    val knownDevice = settings.observeString(Key.DEVICE_ID)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), "")

    init {
        polarBinder.bind(PolarService::class)
    }

    fun startSearch(onlyPolar: Boolean) {
        viewModelScope.launch {
            polarBinder.runWhenConnected { startDeviceSearch(onlyPolar) }
        }
    }

    fun connectDevice(identifier: String) {
        viewModelScope.launch {
            polarBinder.runWhenConnected { connectDevice(identifier) }
        }
    }

    fun disconnectDevice() {
        viewModelScope.launch {
            polarBinder.runWhenConnected { disconnectDevice() }
        }
    }

    fun stopSearch() {
        viewModelScope.launch {
            polarBinder.runWhenConnected { stopServiceSearch() }
        }
    }

    fun saveDevice(deviceId: String) {
        viewModelScope.launch {
            settings.put(Key.DEVICE_ID, deviceId)
        }
    }

    override fun onCleared() {
        polarBinder.unbind()
        super.onCleared()
    }
}
