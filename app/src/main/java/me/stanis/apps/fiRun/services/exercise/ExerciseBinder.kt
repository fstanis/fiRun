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

package me.stanis.apps.fiRun.services.exercise

import android.os.IBinder
import kotlinx.coroutines.flow.SharedFlow
import me.stanis.apps.fiRun.models.AveragePace
import me.stanis.apps.fiRun.models.Calories
import me.stanis.apps.fiRun.models.CurrentPace
import me.stanis.apps.fiRun.models.Distance
import me.stanis.apps.fiRun.models.ExerciseState
import me.stanis.apps.fiRun.models.HeartRate
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.util.errors.ServiceError

interface ExerciseBinder : IBinder {
    suspend fun startExercise(type: ExerciseType, includeHeartRate: Boolean)
    suspend fun endExercise()
    suspend fun resetExercise()
    suspend fun pauseExercise()
    suspend fun resumeExercise()
    val stateUpdates: SharedFlow<ExerciseState>
    val heartRateUpdates: SharedFlow<HeartRate>
    val distanceUpdates: SharedFlow<Distance>
    val caloriesUpdates: SharedFlow<Calories>
    val averagePaceUpdates: SharedFlow<AveragePace>
    val currentPaceUpdates: SharedFlow<CurrentPace>

    val errors: SharedFlow<ServiceError>
}
