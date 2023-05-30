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

import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseLapSummary
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.getCurrentExerciseInfo
import java.time.Duration
import java.time.Instant.now
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal suspend fun ExerciseClient.setExerciseUpdateCallback(onExerciseUpdateReceived: (ExerciseUpdate) -> Unit): ExerciseUpdateCallback =
    suspendCoroutine {
        var callback: ExerciseUpdateCallback? = null
        callback = object : ExerciseUpdateCallback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) =
                onExerciseUpdateReceived(update)

            override fun onRegistered() {
                it.resume(callback!!)
            }

            override fun onRegistrationFailed(throwable: Throwable) {
                it.resumeWithException(throwable)
            }

            // unused
            override fun onAvailabilityChanged(u1: DataType<*, *>, u2: Availability) {}
            override fun onLapSummaryReceived(u: ExerciseLapSummary) {}
        }
        setUpdateCallback(callback)
    }

internal suspend fun ExerciseClient.isOtherAppInProgress() =
    getCurrentExerciseInfo().exerciseTrackedStatus == ExerciseTrackedStatus.OTHER_APP_IN_PROGRESS

internal val ExerciseUpdate.heartRateSamples get() = latestMetrics.getData(DataType.HEART_RATE_BPM)
internal val ExerciseUpdate.totalDistance get() = latestMetrics.getData(DataType.DISTANCE_TOTAL)?.total
internal val ExerciseUpdate.currentPace
    get() = latestMetrics.getData(DataType.PACE).lastOrNull()?.value
internal val ExerciseUpdate.averagePace get() = latestMetrics.getData(DataType.PACE_STATS)?.average
internal val ExerciseUpdate.isActive get() = exerciseStateInfo.state == ExerciseState.ACTIVE
internal val ExerciseUpdate.isPaused get() = exerciseStateInfo.state == ExerciseState.USER_PAUSED
internal val ExerciseUpdate.isEnded get() = exerciseStateInfo.state == ExerciseState.ENDED
internal val ExerciseUpdate.isLoading
    get() = setOf(
        ExerciseState.USER_STARTING,
        ExerciseState.USER_PAUSING,
        ExerciseState.ENDING
    ).contains(exerciseStateInfo.state)
internal val ExerciseUpdate.duration: Duration?
    get() {
        val checkpoint = activeDurationCheckpoint ?: return null
        return if (isActive) {
            checkpoint.activeDuration + Duration.between(checkpoint.time, now())
        } else {
            checkpoint.activeDuration
        }
    }
