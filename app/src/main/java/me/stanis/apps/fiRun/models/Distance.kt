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

import androidx.health.services.client.data.CumulativeDataPoint
import androidx.health.services.client.data.DataType
import java.time.Duration
import java.time.Instant
import me.stanis.apps.fiRun.services.exercise.instant

data class Distance(
    val total: Double,
    override val instant: Instant,
    val exerciseDuration: Duration
) : DataWithInstant {
    companion object {
        fun fromExercise(
            sample: CumulativeDataPoint<Double>?,
            exerciseDuration: Duration
        ): Distance? {
            if (sample?.dataType != DataType.DISTANCE_TOTAL) {
                return null
            }
            return Distance(
                total = sample.total,
                instant = sample.instant,
                exerciseDuration = exerciseDuration
            )
        }
    }
}
