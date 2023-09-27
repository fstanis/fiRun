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

import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.ExerciseUpdate
import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal
import me.stanis.apps.fiRun.models.enums.ExerciseStatus
import me.stanis.apps.fiRun.services.exercise.isActive
import me.stanis.apps.fiRun.services.exercise.isEnded
import me.stanis.apps.fiRun.services.exercise.isLoading
import me.stanis.apps.fiRun.services.exercise.isOtherAppActive
import me.stanis.apps.fiRun.services.exercise.isPaused
import me.stanis.apps.fiRun.util.clock.Clock

data class ExerciseState(
    val status: ExerciseStatus,
    val lastUpdated: Duration,
    val startTime: Instant? = null,
    val lastStateTransitionTime: Instant? = null,
    val activeDuration: Duration? = null
) {
    val isPartial get() = startTime == null || lastStateTransitionTime == null || activeDuration == null

    fun getDuration(now: Temporal): Duration? {
        if (lastStateTransitionTime == null || activeDuration == null) {
            return null
        }
        return if (status == ExerciseStatus.InProgress) {
            activeDuration + Duration.between(lastStateTransitionTime, now)
        } else {
            activeDuration
        }
    }

    companion object {
        fun createWithStatus(state: ExerciseStatus, clock: Clock) =
            ExerciseState(
                status = state,
                lastUpdated = Duration.ofMillis(clock.elapsedRealtime())
            )

        fun fromExerciseUpdate(update: ExerciseUpdate): ExerciseState {
            val activeDurationCheckpoint = update.activeDurationCheckpoint
            val state = when {
                update.isActive -> ExerciseStatus.InProgress
                update.isLoading -> ExerciseStatus.Loading
                update.isEnded -> ExerciseStatus.Ended
                update.isPaused -> ExerciseStatus.Paused
                else -> ExerciseStatus.NotStarted
            }
            return ExerciseState(
                status = state,
                lastUpdated = update.getUpdateDurationFromBoot(),
                startTime = update.startTime,
                lastStateTransitionTime = activeDurationCheckpoint?.time,
                activeDuration = activeDurationCheckpoint?.activeDuration
            )
        }

        fun fromExerciseInfo(exerciseInfo: ExerciseInfo, clock: Clock): ExerciseState {
            val exerciseStatus = when {
                exerciseInfo.isActive -> ExerciseStatus.InProgress
                exerciseInfo.isOtherAppActive -> ExerciseStatus.UsedByDifferentApp
                else -> ExerciseStatus.NotStarted
            }
            return createWithStatus(exerciseStatus, clock)
        }
    }
}
