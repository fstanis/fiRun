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
import androidx.health.services.client.data.ExerciseUpdate
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

internal class ExerciseCallback private constructor(
    private val continuation: Continuation<ExerciseUpdateCallback>,
    private val onExerciseUpdate: (ExerciseUpdate) -> Unit
) : ExerciseUpdateCallback {
    override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
        onExerciseUpdate(update)
    }

    override fun onRegistered() {
        continuation.resume(this)
    }

    override fun onRegistrationFailed(throwable: Throwable) {
        continuation.resumeWithException(throwable)
    }

    // unused
    override fun onAvailabilityChanged(dataType: DataType<*, *>, availability: Availability) {}
    override fun onLapSummaryReceived(lapSummary: ExerciseLapSummary) {}

    companion object {
        suspend fun ExerciseClient.setExerciseUpdateCallback(
            onExerciseUpdate: (ExerciseUpdate) -> Unit
        ): ExerciseUpdateCallback =
            suspendCoroutine {
                setUpdateCallback(ExerciseCallback(it, onExerciseUpdate))
            }
    }
}
