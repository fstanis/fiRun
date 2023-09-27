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

import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.database.dao.DeviceDao
import me.stanis.apps.fiRun.database.entities.DeviceEntity
import me.stanis.apps.fiRun.models.HrDeviceInfo
import me.stanis.apps.fiRun.models.enums.HrDeviceState
import me.stanis.apps.fiRun.services.polar.ConnectionState
import me.stanis.apps.fiRun.services.polar.PolarBinder
import me.stanis.apps.fiRun.services.polar.SearchState
import me.stanis.apps.fiRun.util.binder.BinderConnection
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DeviceManagerTest {
    private val devices = MutableStateFlow(emptyList<DeviceEntity>())
    private val polarBinder = mock<PolarBinder>()

    private val underTest = DeviceManager(
        object : DeviceDao {
            override fun getAll(): Flow<List<DeviceEntity>> = devices

            override fun getLastConnected(): Flow<DeviceEntity?> = devices.map { deviceList ->
                deviceList.filter { it.lastConnected != null }.maxByOrNull { it.lastConnected!! }
            }

            override suspend fun put(vararg deviceEntity: DeviceEntity) {
                devices.update {
                    it.toMutableSet().also { set -> set.addAll(deviceEntity) }.toList()
                }
            }
        },
        object : BinderConnection<PolarBinder>() {
            override val binder = MutableStateFlow(polarBinder)
        }
    )

    @Test
    fun `connectDevice calls connectDevice`() = runTest {
        underTest.connectDevice("foo")

        verify(polarBinder).connectDevice("foo")
    }

    @Test
    fun `searchState returns state from PolarBinder`() = runTest {
        val state = SearchState(SearchState.SearchStatus.AllDevices)
        whenever(polarBinder.searchState).thenReturn(MutableStateFlow(state))

        assertEquals(state, underTest.searchState.first())
    }

    @Test
    fun `knownDevices merges two sources`() = runTest {
        whenever(polarBinder.connectionState).thenReturn(
            MutableStateFlow(
                ConnectionState(
                    setOf(
                        HrDeviceInfo(HrDeviceState.CONNECTED, "foo", "", "")
                    )
                )
            )
        )
        devices.value = listOf(DeviceEntity("bar"), DeviceEntity("baz"))

        assertEquals(
            setOf(
                HrDeviceInfo(HrDeviceState.NOT_CONNECTED, "bar", "", ""),
                HrDeviceInfo(HrDeviceState.NOT_CONNECTED, "baz", "", ""),
                HrDeviceInfo(HrDeviceState.CONNECTED, "foo", "", "")
            ),
            underTest.knownDevices.first()
        )
        assertEquals(
            listOf(DeviceEntity("bar"), DeviceEntity("baz"), DeviceEntity("foo")),
            devices.value
        )
    }
}
