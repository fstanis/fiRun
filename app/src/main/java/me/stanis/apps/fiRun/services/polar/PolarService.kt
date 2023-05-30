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

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.model.PolarDeviceInfo
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import me.stanis.apps.fiRun.util.heartrate.HeartRate
import me.stanis.apps.fiRun.util.notifications.Notifications
import me.stanis.apps.fiRun.util.settings.Settings
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class PolarService : LifecycleService() {
    @Inject
    lateinit var api: PolarBleApi

    @Inject
    lateinit var settings: Settings

    @Inject
    lateinit var notifications: Notifications

    private val mutableDeviceStatus =
        MutableStateFlow<DeviceConnectionStatus>(DeviceConnectionStatus.NotConnected)
    private val mutableDevicesFound = MutableStateFlow<Set<PolarDeviceInfo>>(emptySet())
    private val mutableSearchStatus = MutableStateFlow(SearchStatus.NoSearch)
    private var searchJob: Job? = null

    private val lastHeartRate = mutableDeviceStatus
        .map {
            if ((it as? DeviceConnectionStatus.Connected)?.supportsHr == true) {
                it
            } else {
                null
            }
        }
        .flatMapLatest { status ->
            status?.deviceId?.let { api.startHrStreaming(it).onErrorComplete().asFlow() }
                ?: emptyFlow()
        }
        .mapNotNull {
            HeartRate.fromPolarHrData(it)
        }
        .stateIn(lifecycleScope, SharingStarted.Lazily, null)

    private val binder = object : Binder(), PolarBinder {
        override val deviceStatus: StateFlow<DeviceConnectionStatus> get() = mutableDeviceStatus
        override val devicesFound: StateFlow<Set<PolarDeviceInfo>> get() = mutableDevicesFound
        override val searchStatus: StateFlow<SearchStatus> get() = mutableSearchStatus
        override val lastHeartRate get() = this@PolarService.lastHeartRate
        override fun startDeviceSearch(onlyPolar: Boolean) =
            this@PolarService.startDeviceSearch(onlyPolar)

        override fun stopServiceSearch() = this@PolarService.stopServiceSearch()
        override fun connectDevice(identifier: String) = this@PolarService.connectDevice(identifier)
        override fun disconnectDevice() = this@PolarService.disconnectDevice()
    }

    private fun startDeviceSearch(onlyPolar: Boolean) {
        val newStatus = if (onlyPolar) SearchStatus.OnlyPolar else SearchStatus.AllDevices
        if (!mutableSearchStatus.compareAndSet(SearchStatus.NoSearch, newStatus)) {
            return
        }
        mutableDevicesFound.value = emptySet()
        searchJob?.cancel()
        api.setPolarFilter(onlyPolar)
        searchJob = lifecycleScope.launch {
            api.searchForDevice().asFlow().collect { device ->
                mutableDevicesFound.update { devices ->
                    devices.toMutableSet().also { it.add(device) }
                }
            }
        }
    }

    private fun stopServiceSearch() {
        mutableSearchStatus.value = SearchStatus.NoSearch
        searchJob?.cancel()
    }

    private fun connectDevice(identifier: String) {
        if (mutableDeviceStatus.value.deviceId == identifier) {
            return
        }
        disconnectDevice()
        api.connectToDevice(identifier)
    }

    private fun disconnectDevice() {
        mutableDeviceStatus.value.deviceId?.let { api.disconnectFromDevice(it) }
    }

    private val callback = object : PolarBleApiCallback() {
        override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
            mutableDeviceStatus.value = DeviceConnectionStatus.Connected(
                DeviceConnectionStatus.DeviceInfo(polarDeviceInfo)
            )
        }

        override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
            mutableDeviceStatus.value = DeviceConnectionStatus.Connecting(polarDeviceInfo.deviceId)
        }

        override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
            mutableDeviceStatus.value = DeviceConnectionStatus.NotConnected
        }

        override fun bleSdkFeatureReady(
            identifier: String,
            feature: PolarBleApi.PolarBleSdkFeature
        ) {
            updateConnectedDeviceInfo(identifier) {
                it.copy(
                    features = it.features.union(setOf(feature)),
                )
            }
        }

        override fun disInformationReceived(identifier: String, uuid: UUID, value: String) {
            updateConnectedDeviceInfo(identifier) {
                it.copy(
                    dis = it.dis.toMutableMap().also { dis -> dis[uuid] = value })
            }
        }

        override fun batteryLevelReceived(identifier: String, level: Int) {
            updateConnectedDeviceInfo(identifier) { it.copy(batteryLevel = level) }
        }
    }

    private fun updateConnectedDeviceInfo(
        identifier: String,
        function: (DeviceConnectionStatus.DeviceInfo) -> DeviceConnectionStatus.DeviceInfo
    ) {
        mutableDeviceStatus.update { status ->
            (status as? DeviceConnectionStatus.Connected)?.let {
                if (it.deviceInfo.info.deviceId == identifier) {
                    DeviceConnectionStatus.Connected(
                        deviceInfo = function(it.deviceInfo)
                    )
                } else {
                    it
                }
            } ?: status
        }
    }

    private fun moveToForeground() {
        val intent = Intent(this, this::class.java).also { it.action = ACTION_START_FOREGROUND }
        startForegroundService(intent)
    }

    private fun removeFromForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        api.setApiCallback(callback)
        lifecycleScope.launch {
            mutableDeviceStatus.collect {
                if (it is DeviceConnectionStatus.Connected) {
                    moveToForeground()
                } else {
                    removeFromForeground()
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_FOREGROUND) {
            startForeground(1, notifications.createPolarNotification("Polar"))
            return super.onStartCommand(intent, flags, startId)
        }
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    companion object {
        private const val ACTION_START_FOREGROUND = "start_foreground"
    }
}
