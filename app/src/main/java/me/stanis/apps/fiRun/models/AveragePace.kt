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

package me.stanis.apps.fiRun.models

import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.StatisticalDataPoint
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong
import me.stanis.apps.fiRun.services.exercise.instant

data class AveragePace(
    val average: Duration,
    override val instant: Instant,
    val exerciseDuration: Duration,
    val isDerived: Boolean
) : DataWithInstant {
    companion object {
        fun fromExerciseSpeed(
            sample: StatisticalDataPoint<Double>?,
            exerciseDuration: Duration
        ): AveragePace? {
            if (sample?.dataType != DataType.SPEED_STATS) {
                return null
            }
            return AveragePace(
                average = if (sample.average > 0.0) {
                    Duration.ofMillis(
                        (1_000_000.0 / sample.average).roundToLong()
                    )
                } else {
                    Duration.ZERO
                },
                instant = sample.instant,
                exerciseDuration = exerciseDuration,
                isDerived = false
            )
        }

        fun fromExercisePace(
            sample: StatisticalDataPoint<Double>?,
            exerciseDuration: Duration
        ): AveragePace? {
            if (sample?.dataType != DataType.PACE_STATS) {
                return null
            }
            return AveragePace(
                average = Duration.ofMillis(sample.average.roundToLong()),
                instant = sample.instant,
                exerciseDuration = exerciseDuration,
                isDerived = false
            )
        }

        fun fromDistance(distance: Distance) =
            AveragePace(
                average = if (distance.total > 0.0) {
                    Duration.ofMillis(
                        (
                            distance.exerciseDuration.toMillis()
                                .toDouble() / distance.total * 1000.0
                            ).roundToLong()
                    )
                } else {
                    Duration.ZERO
                },
                instant = distance.instant,
                exerciseDuration = distance.exerciseDuration,
                isDerived = true
            )
    }
}
