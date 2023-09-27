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
import androidx.health.services.client.data.IntervalDataPoint
import androidx.health.services.client.data.SampleDataPoint
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToLong
import me.stanis.apps.fiRun.services.exercise.instant
import me.stanis.apps.fiRun.util.clock.Clock

data class CurrentPace(
    val current: Duration,
    override val instant: Instant,
    val exerciseDuration: Duration,
    val isDerived: Boolean
) : DataWithInstant {
    companion object {
        fun fromExerciseSpeed(
            sample: SampleDataPoint<Double>?,
            exerciseDuration: Duration,
            clock: Clock
        ): CurrentPace? {
            if (sample?.dataType != DataType.SPEED) {
                return null
            }
            return CurrentPace(
                current = if (sample.value > 0.0) {
                    Duration.ofMillis(
                        (1_000_000.0 / sample.value).roundToLong()
                    )
                } else {
                    Duration.ZERO
                },
                instant = sample.instant(clock),
                exerciseDuration = exerciseDuration,
                isDerived = false
            )
        }

        fun fromExercisePace(
            sample: SampleDataPoint<Double>?,
            exerciseDuration: Duration,
            clock: Clock
        ): CurrentPace? {
            if (sample?.dataType != DataType.PACE) {
                return null
            }
            return CurrentPace(
                current = Duration.ofMillis(sample.value.roundToLong()),
                instant = sample.instant(clock),
                exerciseDuration = exerciseDuration,
                isDerived = false
            )
        }

        fun fromExerciseDistance(
            sample: IntervalDataPoint<Double>?,
            exerciseDuration: Duration,
            clock: Clock
        ): CurrentPace? {
            if (sample?.dataType != DataType.DISTANCE) {
                return null
            }
            val duration = sample.endDurationFromBoot - sample.startDurationFromBoot
            return CurrentPace(
                current = if (sample.value > 0.0) {
                    Duration.ofMillis(
                        (duration.toMillis().toDouble() / sample.value * 1000.0).roundToLong()
                    )
                } else {
                    Duration.ZERO
                },
                instant = sample.instant(clock),
                exerciseDuration = exerciseDuration,
                isDerived = true
            )
        }

        fun betweenDistances(first: Distance, second: Distance): CurrentPace {
            val durationDiff = second.exerciseDuration - first.exerciseDuration
            val distanceDiff = second.total - first.total
            return CurrentPace(
                current = if (distanceDiff > 0.0) {
                    Duration.ofMillis(
                        (durationDiff.toMillis().toDouble() / distanceDiff * 1000.0).roundToLong()
                    )
                } else {
                    Duration.ZERO
                },
                instant = second.instant,
                exerciseDuration = second.exerciseDuration,
                isDerived = true
            )
        }
    }
}
