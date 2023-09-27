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

@file:OptIn(ExperimentalCoroutinesApi::class)

package me.stanis.apps.fiRun.services.polar

import android.app.Notification
import android.os.Binder
import com.polar.sdk.api.PolarBleApi
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import me.stanis.apps.fiRun.models.HeartRate
import me.stanis.apps.fiRun.models.HrDeviceInfo
import me.stanis.apps.fiRun.models.enums.HrDeviceState
import me.stanis.apps.fiRun.persistence.ExerciseWriter
import me.stanis.apps.fiRun.services.ForegroundableService
import me.stanis.apps.fiRun.util.clock.Clock
import me.stanis.apps.fiRun.util.errors.ServiceError
import me.stanis.apps.fiRun.util.notifications.Notifications

@AndroidEntryPoint
class PolarService : ForegroundableService<PolarBinder>() {
    @Inject
    lateinit var api: PolarBleApi

    @Inject
    lateinit var notifications: Notifications

    @Inject
    lateinit var exerciseWriter: ExerciseWriter

    @Inject
    lateinit var clock: Clock

    private val mutableSearchState = MutableStateFlow(SearchState.NO_SEARCH)
    private var searchJob: Job? = null
    private val errors = MutableSharedFlow<ServiceError>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val notification: Notification
        get() = notifications.createPolarNotification(
            "Polar info"
        )

    override fun onCoroutineException(throwable: Throwable) {
        errors.tryEmit(ServiceError.WithThrowable(throwable))
    }

    override val binder: PolarBinder = object : Binder(), PolarBinder {
        override val connectionState: StateFlow<ConnectionState> get() = serviceState
        override val searchState: StateFlow<SearchState> = mutableSearchState.asStateFlow()
        override val heartRateUpdates get() = this@PolarService.heartRateUpdates
        override val errors get() = this@PolarService.errors
        override fun startDeviceSearch(onlyPolar: Boolean) =
            this@PolarService.startDeviceSearch(onlyPolar)

        override fun stopDeviceSearch() = this@PolarService.stopDeviceSearch()
        override fun connectDevice(identifier: String) = this@PolarService.connectDevice(identifier)
        override fun disconnectDevice(identifier: String) =
            this@PolarService.disconnectDevice(identifier)
    }

    private fun startDeviceSearch(onlyPolar: Boolean) {
        if (mutableSearchState.value.status != SearchState.SearchStatus.NoSearch) {
            return
        }
        val newStatus =
            if (onlyPolar) SearchState.SearchStatus.OnlyPolar else SearchState.SearchStatus.AllDevices
        searchJob?.cancel()
        mutableSearchState.value = SearchState(newStatus)
        api.setPolarFilter(onlyPolar)
        searchJob = scope.launch {
            api.searchForDevice().doOnError { errors.tryEmit(ServiceError.WithThrowable(it)) }
                .asFlow()
                .map(HrDeviceInfo::fromPolarInfo)
                .collect { device ->
                    mutableSearchState.update { it.withDevice(device) }
                }
        }
    }

    private fun stopDeviceSearch() {
        mutableSearchState.update { it.copy(status = SearchState.SearchStatus.NoSearch) }
        searchJob?.cancel()
    }

    private fun connectDevice(identifier: String) {
        api.connectToDevice(identifier)
        val otherDevices = connectedDevices.value.map { it.deviceId }.filter { it != identifier }
        for (deviceId in otherDevices) {
            disconnectDevice(deviceId)
        }
    }

    private fun disconnectDevice(identifier: String) {
        api.disconnectFromDevice(identifier)
    }

    private val connectedDevices = MutableStateFlow<Set<HrDeviceInfo>>(emptySet())

    private val serviceState = connectedDevices.map {
        ConnectionState(it)
    }
        .stateIn(scope, SharingStarted.Eagerly, ConnectionState.INITIAL)

    private val hasConnectedDevices = connectedDevices.map {
        it.any { device -> device.state == HrDeviceState.CONNECTED }
    }.conflate().distinctUntilChanged()

    private val heartRateUpdates = connectedDevices
        .map {
            it
                .filter { device -> device.canStreamHr }
                .map { device -> device.deviceId }
                .toSet()
        }
        .distinctUntilChanged()
        .flatMapLatest {
            merge(
                *it.map { deviceId ->
                    api.startHrStreaming(deviceId)
                        .onErrorComplete { error ->
                            errors.tryEmit(ServiceError.WithThrowable(error))
                            true
                        }
                        .asFlow()
                        .transform { data ->
                            for (sample in data.samples) {
                                val hr = HeartRate.fromPolarHrSample(deviceId, sample, clock)
                                if (hr != null) {
                                    emit(hr)
                                }
                            }
                        }
                }.toTypedArray()
            )
        }
        .shareIn(scope, SharingStarted.WhileSubscribed())

    override fun onCreate() {
        super.onCreate()
        api.setAutomaticReconnection(true)
        api.setApiCallback(
            DevicesCallback(clock) {
                connectedDevices.value = it.values.toSet()
            }
        )
        scope.launch {
            launch {
                hasConnectedDevices.collect {
                    if (it) {
                        moveToForeground()
                    } else {
                        removeFromForeground()
                    }
                }
            }
            launch { exerciseWriter.writeHeartRateUpdates(heartRateUpdates) }
        }
    }
}
