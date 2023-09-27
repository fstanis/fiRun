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

package me.stanis.apps.fiRun.ui.exercise

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.stanis.apps.fiRun.database.datastore.SettingsData
import me.stanis.apps.fiRun.models.AveragePace
import me.stanis.apps.fiRun.models.Calories
import me.stanis.apps.fiRun.models.CurrentPace
import me.stanis.apps.fiRun.models.Distance
import me.stanis.apps.fiRun.models.HeartRate
import me.stanis.apps.fiRun.persistence.ExerciseManager
import me.stanis.apps.fiRun.services.polar.ConnectionState
import me.stanis.apps.fiRun.ui.BaseViewModel
import me.stanis.apps.fiRun.ui.exercise.model.LatestStats
import me.stanis.apps.fiRun.ui.exercise.model.UiState
import me.stanis.apps.fiRun.util.clock.Clock

@HiltViewModel
class ExerciseViewModel @Inject constructor(
    private val exerciseManager: ExerciseManager,
    settings: DataStore<SettingsData>,
    clock: Clock
) : BaseViewModel() {
    val keepScreenOn = settings.data.map { it.keepScreenOn }.asState(false)

    val uiStateFlow = combine(
        exerciseManager.exerciseState,
        exerciseManager.polarConnectionState.asState(ConnectionState.INITIAL)
    ) { exerciseStatus, polarConnectionState ->
        UiState(
            exerciseState = exerciseStatus,
            connectionStatus = polarConnectionState.status
        )
    }.asState(UiState.createInitial(clock))

    val latestStatsFlow = combine(
        exerciseManager.latestExerciseHr.asState(),
        exerciseManager.latestPolarHr.asState(),
        exerciseManager.latestDistance.asState(),
        exerciseManager.latestAveragePace.asState(),
        exerciseManager.latestCurrentPace.asState(),
        exerciseManager.latestCalories.asState()
    ) { values ->
        LatestStats(
            exerciseHr = values[0] as HeartRate?,
            polarHr = values[1] as HeartRate?,
            distance = values[2] as Distance?,
            averagePace = values[3] as AveragePace?,
            currentPace = values[4] as CurrentPace?,
            calories = values[5] as Calories?
        )
    }.asState(LatestStats())

    init {
        viewModelScope.launch {
            exerciseManager.errors.collect {
                Log.e("fiRun", it.message)
            }
        }
    }

    fun endExercise() {
        viewModelScope.launch {
            exerciseManager.endExercise()
        }
    }

    fun resetExercise() {
        viewModelScope.launch {
            exerciseManager.resetExercise()
        }
    }

    fun pauseExercise() {
        viewModelScope.launch {
            exerciseManager.pauseExercise()
        }
    }

    fun resumeExercise() {
        viewModelScope.launch {
            exerciseManager.resumeExercise()
        }
    }
}
