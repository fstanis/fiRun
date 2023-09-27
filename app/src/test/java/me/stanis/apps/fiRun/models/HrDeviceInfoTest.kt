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
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import me.stanis.apps.fiRun.database.entities.DeviceEntity
import me.stanis.apps.fiRun.models.enums.HrDeviceState
import org.junit.Test

class HrDeviceInfoTest {
    @Test
    fun `merge replaces values in base`() {
        val base = createBase()
        val underTest = base.merge(
            HrDeviceInfo(
                state = HrDeviceState.CONNECTING,
                deviceId = "otherDeviceId",
                address = "otherAddress",
                name = "otherName"
            )
        )

        assertEquals(
            HrDeviceInfo(
                state = HrDeviceState.CONNECTING,
                deviceId = "otherDeviceId",
                address = "otherAddress",
                rssi = null,
                name = "otherName"
            ),
            underTest
        )
    }

    @Test
    fun `merge uses later lastConnected`() {
        val info1 = createBase().copy(lastConnected = Instant.ofEpochSecond(123))
        val info2 = createBase().copy(lastConnected = Instant.ofEpochSecond(345))
        val underTest = info1.merge(info2)

        assertEquals(Instant.ofEpochSecond(345), underTest.lastConnected)
    }

    @Test
    fun `merge keeps non-null battery level`() {
        val info1 = createBase().copy(batteryLevel = 33)
        val info2 = createBase()
        val underTest = info1.merge(info2)

        assertEquals(33, underTest.batteryLevel)
    }

    @Test
    fun `merge merges dis and features`() {
        val uuid1 = UUID.randomUUID()
        val uuid2 = UUID.randomUUID()
        val info1 = createBase().copy(
            dis = mapOf(uuid1 to "foo"),
            features = setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_HR)
        )
        val info2 = createBase().copy(
            dis = mapOf(uuid2 to "bar"),
            features = setOf(PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO)
        )
        val underTest = info1.merge(info2)

        assertEquals(mapOf(uuid1 to "foo", uuid2 to "bar"), underTest.dis)
        assertEquals(
            setOf(
                PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
                PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO
            ),
            underTest.features
        )
    }

    @Test
    fun `withFeature adds feature`() {
        val base = createBase()
        val underTest = base.withFeature(PolarBleApi.PolarBleSdkFeature.FEATURE_HR)

        assertFalse(base.features.contains(PolarBleApi.PolarBleSdkFeature.FEATURE_HR))
        assertTrue(underTest.features.contains(PolarBleApi.PolarBleSdkFeature.FEATURE_HR))
    }

    @Test
    fun `withDis adds key and value`() {
        val base = createBase()
        val uuid = UUID.randomUUID()
        val underTest = base.withDis(uuid, "value")

        assertTrue(base.dis.isEmpty())
        assertEquals("value", underTest.dis[uuid])
    }

    @Test
    fun `withBatteryLevel adds battery level`() {
        val base = createBase()
        val underTest = base.withBatteryLevel(55)

        assertNull(base.batteryLevel)
        assertEquals(55, underTest.batteryLevel)
    }

    @Test
    fun `fromPolarInfo returns valid info`() {
        val underTest = HrDeviceInfo.fromPolarInfo(
            PolarDeviceInfo(
                deviceId = "deviceId",
                address = "address",
                rssi = 56,
                name = "name",
                isConnectable = true
            )
        )

        assertEquals(
            HrDeviceInfo(
                state = HrDeviceState.NOT_CONNECTED,
                deviceId = "deviceId",
                address = "address",
                rssi = 56,
                name = "name"
            ),
            underTest
        )
    }

    @Test
    fun `fromEntity returns valid info`() {
        val underTest = HrDeviceInfo.fromEntity(
            DeviceEntity(
                identifier = "identifier",
                address = "address",
                name = "name"
            )
        )

        assertEquals(
            HrDeviceInfo(
                state = HrDeviceState.NOT_CONNECTED,
                deviceId = "identifier",
                address = "address",
                rssi = null,
                name = "name"
            ),
            underTest
        )
    }

    private fun createBase() = HrDeviceInfo(
        state = HrDeviceState.NOT_CONNECTED,
        deviceId = "baseDeviceId",
        address = "baseAddress",
        rssi = null,
        name = "baseName"
    )
}
