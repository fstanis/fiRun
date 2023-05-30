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

import java.time.Duration
import kotlin.math.roundToLong

data class ExerciseStatus(
    val state: ExerciseState = ExerciseState.NotStarted,
    val lastUpdated: Duration = Duration.ZERO,
    val exerciseInfo: ExerciseInfo = ExerciseInfo()
) {
    data class ExerciseInfo(
        val duration: Duration = Duration.ZERO,
        val totalDistance: Double = 0.0,
        val currentPace: Double = 0.0,
        val averagePace: Double = 0.0
    ) {
        val derivedPace
            get() = if (!duration.isZero && totalDistance > 0.0) {
                Duration.ofSeconds((duration.seconds / (totalDistance / 1000.0)).roundToLong())
            } else {
                Duration.ZERO
            }
    }

    enum class ExerciseState {
        NotStarted,
        UsedByDifferentApp,
        Loading,
        InProgress,
        Paused,
        Ended
    }
}
