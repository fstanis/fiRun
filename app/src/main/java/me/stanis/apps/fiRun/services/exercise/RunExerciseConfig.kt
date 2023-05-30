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

import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseConfig
import androidx.health.services.client.data.ExerciseType

object RunExerciseConfig {
    fun forType(type: ExerciseBinder.RunType, includeHeartRate: Boolean): ExerciseConfig {
        val builder = when (type) {
            ExerciseBinder.RunType.INDOOR_RUN ->
                ExerciseConfig.builder(ExerciseType.RUNNING_TREADMILL)
                    .setIsAutoPauseAndResumeEnabled(false)

            ExerciseBinder.RunType.OUTDOOR_RUN ->
                ExerciseConfig.builder(ExerciseType.RUNNING)
                    .setIsGpsEnabled(true)
                    .setIsAutoPauseAndResumeEnabled(false)
        }
        val dataTypes = mutableSetOf<DataType<*, *>>(
            DataType.DISTANCE_TOTAL,
            //DataType.PACE,
            //DataType.PACE_STATS
        )
        if (includeHeartRate) {
            dataTypes.add(DataType.HEART_RATE_BPM)
        }
        builder.setDataTypes(dataTypes)
        return builder.build()
    }
}
