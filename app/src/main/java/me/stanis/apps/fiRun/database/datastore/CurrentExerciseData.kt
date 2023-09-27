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

package me.stanis.apps.fiRun.database.datastore

import java.time.Duration
import java.time.Instant
import java.time.temporal.Temporal
import java.util.UUID
import kotlinx.serialization.Serializable
import me.stanis.apps.fiRun.database.util.serializers.DurationSerializer
import me.stanis.apps.fiRun.database.util.serializers.InstantSerializer
import me.stanis.apps.fiRun.database.util.serializers.UUIDSerializer

@Serializable
data class CurrentExerciseData(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID?,
    val inProgress: Boolean,
    @Serializable(with = InstantSerializer::class)
    val lastStateTransitionTime: Instant?,
    @Serializable(with = DurationSerializer::class)
    val activeDuration: Duration?
) {
    fun getDuration(now: Temporal): Duration? {
        if (lastStateTransitionTime == null || activeDuration == null) {
            return null
        }
        return if (inProgress) {
            activeDuration + Duration.between(lastStateTransitionTime, now)
        } else {
            activeDuration
        }
    }

    companion object {
        val EMPTY = CurrentExerciseData(null, false, null, null)
    }
}
