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
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import me.stanis.apps.fiRun.models.HrDeviceInfo
import me.stanis.apps.fiRun.models.enums.HrDeviceState
import me.stanis.apps.fiRun.util.clock.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DevicesCallbackTest {
    private val fakeClock = object : Clock {
        override fun now(): Instant = Instant.ofEpochMilli(123)
        override fun currentTimeMillis(): Long = 0
        override fun elapsedRealtime(): Long = 0
    }

    private var devices = emptyMap<String, HrDeviceInfo>()
    private val callback = DevicesCallback(fakeClock) { devices = it }

    @Test
    fun `deviceConnecting produces correct HrDeviceInfo`() {
        callback.deviceConnecting(PolarDeviceInfo("id", "address", -80, "name", true))

        assertEquals(
            mapOf(
                "id" to HrDeviceInfo(
                    state = HrDeviceState.CONNECTING,
                    deviceId = "id",
                    address = "address",
                    rssi = -80,
                    name = "name"
                )
            ),
            devices
        )
    }

    @Test
    fun `deviceConnected produces correct HrDeviceInfo`() {
        callback.deviceConnected(PolarDeviceInfo("id", "address", -80, "name", true))

        assertEquals(
            mapOf(
                "id" to HrDeviceInfo(
                    state = HrDeviceState.CONNECTED,
                    deviceId = "id",
                    address = "address",
                    rssi = -80,
                    name = "name",
                    lastConnected = fakeClock.now()
                )
            ),
            devices
        )
    }

    @Test
    fun `deviceDisconnected removes HrDeviceInfo`() {
        val device = PolarDeviceInfo("id", "address", -80, "name", true)
        callback.deviceConnected(device)
        callback.deviceDisconnected(device)

        assertEquals(emptyMap(), devices)
    }

    @Test
    fun `bleSdkFeatureReady modifies HrDeviceInfo`() {
        val device = PolarDeviceInfo("id", "address", -80, "name", true)
        callback.deviceConnected(device)
        callback.bleSdkFeatureReady("id", PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO)

        assertEquals(
            mapOf(
                "id" to HrDeviceInfo(
                    state = HrDeviceState.CONNECTED,
                    deviceId = "id",
                    address = "address",
                    rssi = -80,
                    name = "name",
                    lastConnected = fakeClock.now(),
                    features = setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO)
                )
            ),
            devices
        )
    }

    @Test
    fun `disInformationReceived modifies HrDeviceInfo`() {
        val device = PolarDeviceInfo("id", "address", -80, "name", true)
        callback.deviceConnected(device)
        callback.disInformationReceived(
            "id",
            UUID.fromString("00001234-0000-1000-8000-000000000000"),
            "value"
        )

        assertEquals(
            mapOf(
                "id" to HrDeviceInfo(
                    state = HrDeviceState.CONNECTED,
                    deviceId = "id",
                    address = "address",
                    rssi = -80,
                    name = "name",
                    lastConnected = fakeClock.now(),
                    dis = mapOf(
                        UUID.fromString("00001234-0000-1000-8000-000000000000") to "value"
                    )
                )
            ),
            devices
        )
    }

    @Test
    fun `batteryLevelReceived modifies HrDeviceInfo`() {
        val device = PolarDeviceInfo("id", "address", -80, "name", true)
        callback.deviceConnected(device)
        callback.batteryLevelReceived("id", 40)

        assertEquals(
            mapOf(
                "id" to HrDeviceInfo(
                    state = HrDeviceState.CONNECTED,
                    deviceId = "id",
                    address = "address",
                    rssi = -80,
                    name = "name",
                    lastConnected = fakeClock.now(),
                    batteryLevel = 40
                )
            ),
            devices
        )
    }
}
