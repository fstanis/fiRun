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

package me.stanis.apps.fiRun.ui.exercise.model

import me.stanis.apps.fiRun.models.ExerciseState
import me.stanis.apps.fiRun.models.enums.ExerciseStatus
import me.stanis.apps.fiRun.services.polar.ConnectionState
import me.stanis.apps.fiRun.util.clock.Clock

data class UiState(
    val exerciseState: ExerciseState,
    val connectionStatus: ConnectionState.ConnectionStatus
) {
    companion object {
        fun createInitial(clock: Clock) = UiState(
            exerciseState = ExerciseState.createWithStatus(ExerciseStatus.Loading, clock),
            connectionStatus = ConnectionState.ConnectionStatus.INACTIVE
        )
    }
}
