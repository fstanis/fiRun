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

import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.model.PolarDeviceInfo
import java.time.Instant
import java.util.UUID
import me.stanis.apps.fiRun.database.entities.DeviceEntity
import me.stanis.apps.fiRun.models.enums.HrDeviceState

data class HrDeviceInfo(
    val state: HrDeviceState,
    val deviceId: String,
    val address: String,
    val name: String,
    val rssi: Int? = null,
    val features: Set<PolarBleApi.PolarBleSdkFeature> = emptySet(),
    val dis: Map<UUID, String> = emptyMap(),
    val batteryLevel: Int? = null,
    val lastConnected: Instant? = null
) {
    val canStreamHr
        get() = state == HrDeviceState.CONNECTED && features.contains(
            PolarBleApi.PolarBleSdkFeature.FEATURE_HR
        )
    val isPreparing get() = state != HrDeviceState.NOT_CONNECTED && !canStreamHr

    fun merge(other: HrDeviceInfo) =
        HrDeviceInfo(
            state = other.state,
            deviceId = other.deviceId,
            address = other.address,
            name = other.name,
            rssi = other.rssi ?: rssi,
            features = features + other.features,
            dis = dis + other.dis,
            batteryLevel = other.batteryLevel ?: batteryLevel,
            lastConnected =
            if (lastConnected == null) {
                other.lastConnected
            } else if (other.lastConnected == null) {
                lastConnected
            } else {
                if (other.lastConnected.isAfter(lastConnected)) {
                    other.lastConnected
                } else {
                    lastConnected
                }
            }
        )

    fun withDeviceConnecting(polarDeviceInfo: PolarDeviceInfo) =
        copy(
            state = HrDeviceState.CONNECTING,
            deviceId = polarDeviceInfo.deviceId,
            address = polarDeviceInfo.address,
            rssi = polarDeviceInfo.rssi,
            name = polarDeviceInfo.name
        )

    fun withDeviceConnected(polarDeviceInfo: PolarDeviceInfo, now: Instant) =
        copy(
            state = HrDeviceState.CONNECTED,
            deviceId = polarDeviceInfo.deviceId,
            address = polarDeviceInfo.address,
            rssi = polarDeviceInfo.rssi,
            name = polarDeviceInfo.name,
            lastConnected = now
        )

    fun withFeature(feature: PolarBleApi.PolarBleSdkFeature) =
        copy(features = features.union(setOf(feature)))

    fun withDis(uuid: UUID, value: String) =
        copy(dis = dis.toMutableMap().also { dis -> dis[uuid] = value })

    fun withBatteryLevel(batteryLevel: Int) =
        copy(batteryLevel = batteryLevel)

    companion object {
        val EMPTY = HrDeviceInfo(
            state = HrDeviceState.UNKNOWN,
            deviceId = "",
            address = "",
            rssi = null,
            name = ""
        )

        fun fromPolarInfo(polarDeviceInfo: PolarDeviceInfo) = HrDeviceInfo(
            state = HrDeviceState.NOT_CONNECTED,
            deviceId = polarDeviceInfo.deviceId,
            address = polarDeviceInfo.address,
            rssi = polarDeviceInfo.rssi,
            name = polarDeviceInfo.name
        )

        fun fromEntity(deviceEntity: DeviceEntity) = HrDeviceInfo(
            state = HrDeviceState.NOT_CONNECTED,
            deviceId = deviceEntity.identifier,
            name = deviceEntity.name,
            address = deviceEntity.address
        )
    }
}
