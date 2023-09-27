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

package me.stanis.apps.fiRun.ui.home.model

import me.stanis.apps.fiRun.services.polar.ConnectionState

data class UiState(
    val homeStatus: HomeStatus,
    val connectionStatus: ConnectionState.ConnectionStatus,
    val canConnectPolar: Boolean
) {
    enum class HomeStatus {
        Loading,
        Default,
        ExerciseInProgress
    }

    companion object {
        val INITIAL = UiState(
            homeStatus = HomeStatus.Loading,
            connectionStatus = ConnectionState.ConnectionStatus.INACTIVE,
            canConnectPolar = false
        )
    }
}
