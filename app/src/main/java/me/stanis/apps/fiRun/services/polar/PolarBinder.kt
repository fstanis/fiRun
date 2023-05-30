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

import android.os.IBinder
import com.polar.sdk.api.model.PolarDeviceInfo
import kotlinx.coroutines.flow.StateFlow
import me.stanis.apps.fiRun.util.heartrate.HeartRate

interface PolarBinder : IBinder {
    val devicesFound: StateFlow<Set<PolarDeviceInfo>>
    val lastHeartRate: StateFlow<HeartRate?>

    fun startDeviceSearch(onlyPolar: Boolean)
    fun stopServiceSearch()
    val deviceStatus: StateFlow<DeviceConnectionStatus>
    val searchStatus: StateFlow<SearchStatus>
    fun connectDevice(identifier: String)
    fun disconnectDevice()
}
