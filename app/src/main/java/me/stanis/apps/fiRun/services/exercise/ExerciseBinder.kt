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

import android.os.IBinder
import kotlinx.coroutines.flow.StateFlow
import me.stanis.apps.fiRun.util.heartrate.HeartRate

interface ExerciseBinder : IBinder {
    fun startRun(type: RunType, includeHeartRate: Boolean)
    fun endRun()
    fun reset()
    val status: StateFlow<ExerciseStatus>
    val lastHeartRate: StateFlow<HeartRate?>

    enum class RunType {
        INDOOR_RUN,
        OUTDOOR_RUN
    }
}
