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

package me.stanis.apps.fiRun.services.polar

import me.stanis.apps.fiRun.models.HrDeviceInfo

data class ConnectionState(
    val connectedDevices: Set<HrDeviceInfo>
) {
    val canStreamHr get() = connectedDevices.any { it.canStreamHr }
    val isPreparing get() = connectedDevices.isNotEmpty() && !canStreamHr

    val status
        get() = when {
            canStreamHr -> ConnectionStatus.READY
            isPreparing -> ConnectionStatus.PREPARING
            else -> ConnectionStatus.INACTIVE
        }

    enum class ConnectionStatus {
        INACTIVE,
        PREPARING,
        READY
    }

    companion object {
        val INITIAL = ConnectionState(emptySet())
    }
}
