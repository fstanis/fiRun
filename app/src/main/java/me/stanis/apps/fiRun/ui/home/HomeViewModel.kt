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

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.stanis.apps.fiRun.services.BoundServiceRepository
import me.stanis.apps.fiRun.services.exercise.ExerciseBinder
import me.stanis.apps.fiRun.services.exercise.ExerciseService
import me.stanis.apps.fiRun.services.exercise.ExerciseStatus
import me.stanis.apps.fiRun.services.polar.DeviceConnectionStatus
import me.stanis.apps.fiRun.services.polar.PolarBinder
import me.stanis.apps.fiRun.services.polar.PolarService
import me.stanis.apps.fiRun.util.settings.Settings
import me.stanis.apps.fiRun.util.settings.Settings.Companion.Key
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: Settings
) : ViewModel() {
    private val exerciseBinder = BoundServiceRepository<ExerciseBinder>(context)
    private val polarBinder = BoundServiceRepository<PolarBinder>(context)

    val status = exerciseBinder.flowWhenConnected { status }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), ExerciseStatus())

    val deviceConnectionStatus = polarBinder.flowWhenConnected { deviceStatus }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            DeviceConnectionStatus.NotConnected
        )

    val polarHrData = polarBinder.flowWhenConnected { lastHeartRate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val exerciseHrData = exerciseBinder.flowWhenConnected { lastHeartRate }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val canConnectPolar = settings.observeString(Key.DEVICE_ID).map { it.isNotEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        exerciseBinder.bind(ExerciseService::class)
        polarBinder.bind(PolarService::class)
    }

    fun startExercise() {
        viewModelScope.launch {
            exerciseBinder.runWhenConnected {
                startRun(
                    ExerciseBinder.RunType.INDOOR_RUN,
                    deviceConnectionStatus.value is DeviceConnectionStatus.NotConnected
                )
            }
        }
    }

    fun endExercise() {
        viewModelScope.launch {
            exerciseBinder.runWhenConnected { endRun() }
        }
    }

    fun connectPolar() {
        viewModelScope.launch {
            val deviceId = settings.getString(Key.DEVICE_ID)
            if (deviceId.isNotEmpty()) {
                polarBinder.runWhenConnected { connectDevice(deviceId) }
            }
        }
    }

    fun disconnectPolar() {
        viewModelScope.launch {
            polarBinder.runWhenConnected { disconnectDevice() }
        }
    }

    fun resetExercise() {
        viewModelScope.launch {
            exerciseBinder.runWhenConnected { reset() }
        }
    }

    override fun onCleared() {
        exerciseBinder.unbind()
        polarBinder.unbind()
        super.onCleared()
    }
}
