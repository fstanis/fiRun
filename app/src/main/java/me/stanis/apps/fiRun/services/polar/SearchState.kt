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

data class SearchState(
    val status: SearchStatus,
    val devicesFound: Set<HrDeviceInfo> = emptySet()
) {
    fun withDevice(deviceInfo: HrDeviceInfo) =
        copy(
            devicesFound = devicesFound.toMutableSet().also { it.add(deviceInfo) }
        )

    enum class SearchStatus {
        NoSearch,
        AllDevices,
        OnlyPolar
    }

    companion object {
        val NO_SEARCH = SearchState(SearchStatus.NoSearch)
    }
}
