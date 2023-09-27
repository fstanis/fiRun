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

package me.stanis.apps.fiRun.persistence

import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import me.stanis.apps.fiRun.database.dao.DeviceDao
import me.stanis.apps.fiRun.database.entities.DeviceEntity
import me.stanis.apps.fiRun.models.HrDeviceInfo
import me.stanis.apps.fiRun.services.polar.PolarBinder
import me.stanis.apps.fiRun.util.binder.BinderConnection

@ActivityRetainedScoped
class DeviceManager @Inject constructor(
    private val deviceDao: DeviceDao,
    private val polarBinder: BinderConnection<PolarBinder>
) {
    private val connectionState = polarBinder.flowWhenConnected(PolarBinder::connectionState)
    val searchState = polarBinder.flowWhenConnected(PolarBinder::searchState)
    val lastConnectedDevice = deviceDao.getLastConnected()
    private val connectedDevices = connectionState.map { it.connectedDevices }
    private val savedDevices = deviceDao.getAll().map { devices ->
        devices.map(HrDeviceInfo::fromEntity).toSet()
    }
    val knownDevices =
        combine(connectedDevices, savedDevices) { connectedDevices, savedDevices ->
            val knownDevicesMap = savedDevices.associateBy { it.deviceId }.toMutableMap()
            for (connectedDevice in connectedDevices) {
                knownDevicesMap.compute(connectedDevice.deviceId) { _, value ->
                    value?.merge(connectedDevice) ?: connectedDevice
                }
            }
            knownDevicesMap.values.toSet()
        }
            .onEach { devices ->
                saveDevices(devices)
            }

    suspend fun startSearch(onlyPolar: Boolean) {
        polarBinder.runWhenConnected { it.startDeviceSearch(onlyPolar) }
    }

    suspend fun connectDevice(identifier: String) {
        polarBinder.runWhenConnected { it.connectDevice(identifier) }
    }

    suspend fun disconnectDevice(identifier: String) {
        polarBinder.runWhenConnected { it.disconnectDevice(identifier) }
    }

    suspend fun stopSearch() {
        polarBinder.runWhenConnected { it.stopDeviceSearch() }
    }

    suspend fun saveDevice(device: HrDeviceInfo) {
        saveDevices(listOf(device))
    }

    private suspend fun saveDevices(devices: Iterable<HrDeviceInfo>) {
        deviceDao.put(
            *devices.map { device ->
                DeviceEntity(
                    identifier = device.deviceId,
                    name = device.name,
                    address = device.address,
                    lastConnected = device.lastConnected
                )
            }.toTypedArray()
        )
    }
}
