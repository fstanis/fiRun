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

package me.stanis.apps.fiRun.ui.home

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.persistence.DeviceManager
import me.stanis.apps.fiRun.persistence.ExerciseManager
import me.stanis.apps.fiRun.ui.BaseViewModel
import me.stanis.apps.fiRun.ui.home.model.UiState

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val deviceManager: DeviceManager,
    private val exerciseManager: ExerciseManager
) : BaseViewModel() {
    private val lastConnectedDevice =
        deviceManager.lastConnectedDevice.map { it?.identifier }.asState()

    val uiStateFlow = combine(
        exerciseManager.exerciseState,
        exerciseManager.polarConnectionState,
        lastConnectedDevice
    ) { exerciseStatus, polarConnectionState, lastConnectedDevice ->
        UiState(
            homeStatus = if (exerciseStatus.status.isActive) {
                UiState.HomeStatus.ExerciseInProgress
            } else {
                UiState.HomeStatus.Default
            },
            connectionStatus = polarConnectionState.status,
            canConnectPolar = lastConnectedDevice != null
        )
    }.asState(UiState.INITIAL)

    fun startExercise(type: ExerciseType) {
        viewModelScope.launch {
            exerciseManager.startExercise(type)
        }
    }

    fun connectPolar() {
        viewModelScope.launch {
            val deviceId = lastConnectedDevice.value
            if (deviceId != null) {
                deviceManager.connectDevice(deviceId)
            }
        }
    }

    fun disconnectPolar() {
        viewModelScope.launch {
            val deviceId = lastConnectedDevice.value
            if (deviceId != null) {
                deviceManager.disconnectDevice(deviceId)
            }
        }
    }
}
