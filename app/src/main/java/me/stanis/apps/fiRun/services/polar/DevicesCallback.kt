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
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.model.PolarDeviceInfo
import java.util.UUID
import me.stanis.apps.fiRun.models.HrDeviceInfo
import me.stanis.apps.fiRun.util.clock.Clock

class DevicesCallback(
    private val clock: Clock,
    private val onUpdate: (Map<String, HrDeviceInfo>) -> Unit
) : PolarBleApiCallback() {
    private val connectedDevices = mutableMapOf<String, HrDeviceInfo>()

    override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
        val deviceId = polarDeviceInfo.deviceId
        connectedDevices.compute(deviceId) { _, info ->
            (info ?: HrDeviceInfo.EMPTY).withDeviceConnected(
                polarDeviceInfo,
                clock.now()
            )
        }
        onUpdate(connectedDevices)
    }

    override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
        val deviceId = polarDeviceInfo.deviceId
        connectedDevices.compute(deviceId) { _, info ->
            (info ?: HrDeviceInfo.EMPTY).withDeviceConnecting(polarDeviceInfo)
        }
        onUpdate(connectedDevices)
    }

    override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
        val deviceId = polarDeviceInfo.deviceId
        connectedDevices.remove(deviceId)
        onUpdate(connectedDevices)
    }

    override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {
        connectedDevices.compute(identifier) { _, info ->
            info?.withFeature(feature)
        }
        onUpdate(connectedDevices)
    }

    override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
        connectedDevices.compute(identifier) { _, info ->
            info?.withDis(uuid, value)
        }
        onUpdate(connectedDevices)
    }

    override fun batteryLevelReceived(identifier: String, level: Int) {
        connectedDevices.compute(identifier) { _, info ->
            info?.withBatteryLevel(level)
        }
        onUpdate(connectedDevices)
    }
}
