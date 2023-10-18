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

package me.stanis.apps.fiRun.persistence

import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.merge
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.services.exercise.ExerciseBinder
import me.stanis.apps.fiRun.services.polar.PolarBinder
import me.stanis.apps.fiRun.util.binder.BinderConnection

@ActivityRetainedScoped
class ExerciseManager @Inject constructor(
    private val exerciseBinder: BinderConnection<out ExerciseBinder>,
    polarBinder: BinderConnection<out PolarBinder>
) {
    val exerciseState = exerciseBinder.flowWhenConnected(ExerciseBinder::stateUpdates)
    val polarConnectionState = polarBinder.flowWhenConnected(PolarBinder::connectionState)

    val latestPolarHr = polarBinder.flowWhenConnected(PolarBinder::heartRateUpdates)
    val latestExerciseHr = exerciseBinder.flowWhenConnected(ExerciseBinder::heartRateUpdates)

    val latestDistance = exerciseBinder.flowWhenConnected(ExerciseBinder::distanceUpdates)
    val latestCalories = exerciseBinder.flowWhenConnected(ExerciseBinder::caloriesUpdates)

    val latestAveragePace = exerciseBinder.flowWhenConnected(ExerciseBinder::averagePaceUpdates)
    val latestCurrentPace = exerciseBinder.flowWhenConnected(ExerciseBinder::currentPaceUpdates)

    val errors = merge(
        exerciseBinder.flowWhenConnected(ExerciseBinder::errors),
        polarBinder.flowWhenConnected(PolarBinder::errors)
    )

    suspend fun startExercise(type: ExerciseType) {
        exerciseBinder.runWhenConnected { it.startExercise(type, true) }
    }

    suspend fun endExercise() {
        exerciseBinder.runWhenConnected { it.endExercise() }
    }

    suspend fun resetExercise() {
        exerciseBinder.runWhenConnected { it.resetExercise() }
    }

    suspend fun pauseExercise() {
        exerciseBinder.runWhenConnected { it.pauseExercise() }
    }

    suspend fun resumeExercise() {
        exerciseBinder.runWhenConnected { it.resumeExercise() }
    }
}
