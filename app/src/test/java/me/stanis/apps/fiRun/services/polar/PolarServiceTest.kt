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

import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.model.PolarDeviceInfo
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import io.reactivex.rxjava3.processors.UnicastProcessor
import java.time.Instant
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.models.HrDeviceInfo
import me.stanis.apps.fiRun.models.enums.HrDeviceState
import me.stanis.apps.fiRun.modules.ClockModule
import me.stanis.apps.fiRun.modules.PolarBleModule
import me.stanis.apps.fiRun.util.binder.BinderConnectionHelper.createBinderConnection
import me.stanis.apps.fiRun.util.binder.ServiceBinderConnection
import me.stanis.apps.fiRun.util.clock.Clock
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@UninstallModules(PolarBleModule::class, ClockModule::class)
@RunWith(RobolectricTestRunner::class)
class PolarServiceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val mockApi: PolarBleApi = mock()

    @BindValue
    val fakeClock = object : Clock {
        override fun now(): Instant = Instant.ofEpochMilli(123)
        override fun currentTimeMillis(): Long = 0
        override fun elapsedRealtime(): Long = 0
    }

    private val testScope = TestScope()

    private lateinit var binder: ServiceBinderConnection<PolarBinder>
    private lateinit var mockCallback: PolarBleApiCallback

    @Before
    fun setUp() {
        hiltRule.inject()
        binder = createBinderConnection<PolarBinder, PolarService>()
        val captor = argumentCaptor<PolarBleApiCallback>()
        verify(mockApi).setApiCallback(captor.capture())
        mockCallback = captor.firstValue
    }

    @After
    fun tearDown() {
        binder.unbind()
    }

    @Test
    fun `connectDevice calls underlying API`() = testScope.runTest {
        binder.runWhenConnected { it.connectDevice("id") }
        verify(mockApi).connectToDevice(eq("id"))
    }

    @Test
    fun `connectDevice disconnects other devices`() = testScope.runTest {
        val device = PolarDeviceInfo("id1", "", 0, "", true)
        mockCallback.deviceConnected(device)
        runCurrent()
        binder.runWhenConnected { it.connectDevice("id2") }

        verify(mockApi).connectToDevice(eq("id2"))
        verify(mockApi).disconnectFromDevice(eq("id1"))
    }

    @Test
    fun `callbacks correctly change state`() = testScope.runTest {
        val state = binder.flowWhenConnected(PolarBinder::connectionState).stateIn(
            backgroundScope,
            SharingStarted.Eagerly,
            ConnectionState.INITIAL
        )
        runCurrent()
        assertEquals(ConnectionState.ConnectionStatus.INACTIVE, state.value.status)

        val device = PolarDeviceInfo("id", "address", -80, "name", true)
        mockCallback.deviceConnecting(device)
        runCurrent()
        assertEquals(ConnectionState.ConnectionStatus.PREPARING, state.value.status)
        assertEquals(
            setOf(
                HrDeviceInfo(
                    state = HrDeviceState.CONNECTING,
                    deviceId = "id",
                    address = "address",
                    rssi = -80,
                    name = "name"
                )
            ),
            state.value.connectedDevices
        )

        mockCallback.deviceConnected(device)
        val lastConnected = fakeClock.now()
        runCurrent()
        assertEquals(ConnectionState.ConnectionStatus.PREPARING, state.value.status)
        assertEquals(
            setOf(
                HrDeviceInfo(
                    state = HrDeviceState.CONNECTED,
                    deviceId = "id",
                    address = "address",
                    rssi = -80,
                    name = "name",
                    lastConnected = lastConnected
                )
            ),
            state.value.connectedDevices
        )

        mockCallback.bleSdkFeatureReady(device.deviceId, PolarBleApi.PolarBleSdkFeature.FEATURE_HR)
        runCurrent()
        assertEquals(ConnectionState.ConnectionStatus.READY, state.value.status)
        assertEquals(
            setOf(
                HrDeviceInfo(
                    state = HrDeviceState.CONNECTED,
                    deviceId = "id",
                    address = "address",
                    rssi = -80,
                    name = "name",
                    features = setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_HR),
                    lastConnected = lastConnected
                )
            ),
            state.value.connectedDevices
        )

        mockCallback.deviceDisconnected(device)
        runCurrent()
        assertEquals(ConnectionState.ConnectionStatus.INACTIVE, state.value.status)
        assertEquals(emptySet(), state.value.connectedDevices)
    }

    @Test
    fun `startDeviceSearch returns flow of detected devices`() = testScope.runTest {
        val device1 = PolarDeviceInfo("deviceId1", "address1", -80, "name1", true)
        val device2 = PolarDeviceInfo("deviceId2", "address2", -80, "name2", true)
        val polarDevices = UnicastProcessor.create<PolarDeviceInfo>()
        whenever(mockApi.searchForDevice()).thenReturn(polarDevices)

        val searchState = binder.flowWhenConnected(PolarBinder::searchState).stateIn(
            backgroundScope,
            SharingStarted.Eagerly,
            SearchState.NO_SEARCH
        )
        runCurrent()
        assertEquals(SearchState.NO_SEARCH, searchState.value)

        binder.runWhenConnected { it.startDeviceSearch(true) }
        runCurrent()
        assertEquals(SearchState.SearchStatus.OnlyPolar, searchState.value.status)
        assert(searchState.value.devicesFound.isEmpty())

        polarDevices.onNext(device1)
        runCurrent()
        assertEquals(setOf(HrDeviceInfo.fromPolarInfo(device1)), searchState.value.devicesFound)

        polarDevices.onNext(device2)
        runCurrent()
        assertEquals(
            setOf(
                HrDeviceInfo.fromPolarInfo(device1),
                HrDeviceInfo.fromPolarInfo(device2)
            ),
            searchState.value.devicesFound
        )

        binder.runWhenConnected { it.stopDeviceSearch() }
        runCurrent()
        assertEquals(SearchState.SearchStatus.NoSearch, searchState.value.status)
        assert(searchState.value.devicesFound.isNotEmpty())

        binder.runWhenConnected { it.startDeviceSearch(false) }
        runCurrent()
        assertEquals(SearchState.SearchStatus.AllDevices, searchState.value.status)
        assert(searchState.value.devicesFound.isEmpty())
    }
}
