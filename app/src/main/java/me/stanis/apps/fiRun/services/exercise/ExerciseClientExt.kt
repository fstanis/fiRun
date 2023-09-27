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

import androidx.health.services.client.data.CumulativeDataPoint
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseEndReason
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.HeartRateAccuracy
import androidx.health.services.client.data.IntervalDataPoint
import androidx.health.services.client.data.SampleDataPoint
import androidx.health.services.client.data.StatisticalDataPoint
import java.time.Instant
import me.stanis.apps.fiRun.util.clock.Clock

internal val ExerciseUpdate.heartRateSamples get() = latestMetrics.getData(DataType.HEART_RATE_BPM)
internal val ExerciseUpdate.totalDistance get() = latestMetrics.getData(DataType.DISTANCE_TOTAL)
internal val ExerciseUpdate.totalCalories get() = latestMetrics.getData(DataType.CALORIES_TOTAL)
internal val ExerciseUpdate.speedSamples get() = latestMetrics.getData(DataType.SPEED)
internal val ExerciseUpdate.speedStats get() = latestMetrics.getData(DataType.SPEED_STATS)
internal val ExerciseUpdate.paceSamples get() = latestMetrics.getData(DataType.PACE)
internal val ExerciseUpdate.paceStats get() = latestMetrics.getData(DataType.PACE_STATS)

internal val ExerciseUpdate.isActive get() = exerciseStateInfo.state == ExerciseState.ACTIVE
internal val ExerciseUpdate.isPaused get() = exerciseStateInfo.state.isPaused
internal val ExerciseUpdate.isEnded get() = exerciseStateInfo.state.isEnded
internal val ExerciseUpdate.isEndedInError
    get() =
        isEnded && exerciseStateInfo.endReason != ExerciseEndReason.USER_END
internal val ExerciseUpdate.isLoading
    get() = exerciseStateInfo.state.isResuming || exerciseStateInfo.state.isEnding || setOf(
        ExerciseState.USER_STARTING,
        ExerciseState.USER_PAUSING
    ).contains(exerciseStateInfo.state)

private fun bootInstant(clock: Clock) = Instant.ofEpochMilli(
    clock.currentTimeMillis() - clock.elapsedRealtime()
)

internal fun SampleDataPoint<*>.instant(clock: Clock): Instant = getTimeInstant(bootInstant(clock))
internal fun IntervalDataPoint<*>.instant(clock: Clock): Instant = getEndInstant(bootInstant(clock))
internal val CumulativeDataPoint<*>.instant: Instant get() = end
internal val StatisticalDataPoint<*>.instant: Instant get() = end
internal val ExerciseInfo.isActive
    get() =
        exerciseTrackedStatus == ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS
internal val ExerciseInfo.isOtherAppActive
    get() =
        exerciseTrackedStatus == ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS
internal val SampleDataPoint<Double>.sensorStatus
    get() = (accuracy as? HeartRateAccuracy)?.sensorStatus ?: HeartRateAccuracy.SensorStatus.UNKNOWN
