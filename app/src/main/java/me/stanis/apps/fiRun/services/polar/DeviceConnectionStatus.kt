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

import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.model.PolarDeviceInfo
import java.util.UUID

sealed class DeviceConnectionStatus {
    abstract val deviceId: String?

    object NotConnected : DeviceConnectionStatus() {
        override val deviceId: String? get() = null
    }

    data class Connecting(override val deviceId: String) : DeviceConnectionStatus()
    data class Connected(val deviceInfo: DeviceInfo) : DeviceConnectionStatus() {
        override val deviceId: String get() = deviceInfo.info.deviceId
        val supportsHr
            get() = deviceInfo.features.contains(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR
            )
    }

    data class DeviceInfo(
        val info: PolarDeviceInfo,
        val features: Set<PolarBleApi.PolarBleSdkFeature> = emptySet(),
        val dis: Map<UUID, String> = emptyMap(),
        val batteryLevel: Int? = null
    )
}
