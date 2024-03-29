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

package me.stanis.apps.fiRun.util.binder

import android.app.Application
import android.app.Service
import android.content.ComponentName
import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.services.TestRetainedLifecycle
import me.stanis.apps.fiRun.util.binder.ServiceBinderConnection.Companion.bindService
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class ServiceBinderConnectionTest {
    private val app: Application = getApplicationContext()
    private val shadowApp = shadowOf(app)
    private val testLifecycle = TestRetainedLifecycle()

    @Before
    fun setUp() {
        shadowApp.setBindServiceCallsOnServiceConnectedDirectly(true)
    }

    @After
    fun tearDown() {
        testLifecycle.dispatchOnCleared()
    }

    @Test
    fun `runWhenConnected runs when binder is connected`() = runTest {
        shadowApp.setComponentNameAndServiceForBindService(
            ComponentName(app, ValueBinderService::class.java),
            ServiceBinderConnection.BinderWrapper(ValueBinderService())
        )
        val underTest = testLifecycle.bindService<ValueBinderService>(app)

        underTest.runWhenConnected {
            assertEquals(10, it.returnValue(10))
        }

        assertNotNull(underTest.binder.value)
    }

    @Test
    fun `flowWhenConnected returns flow when binder is connected`() = runTest {
        shadowApp.setComponentNameAndServiceForBindService(
            ComponentName(app, FlowBinderService::class.java),
            ServiceBinderConnection.BinderWrapper(FlowBinderService())
        )
        val underTest = testLifecycle.bindService<FlowBinderService>(app)

        val flow = underTest.flowWhenConnected(FlowBinder::flow)

        assertContentEquals(listOf(1, 2, 3), flow.take(3).toList())
    }

    @Test
    fun `unbind disconnects and sets to null`() {
        shadowApp.setComponentNameAndServiceForBindService(
            ComponentName(app, ValueBinderService::class.java),
            ServiceBinderConnection.BinderWrapper(ValueBinderService())
        )
        val underTest = testLifecycle.bindService<ValueBinderService>(app)

        assertEquals(underTest, shadowApp.boundServiceConnections.first())

        underTest.unbind()

        assertTrue(shadowApp.boundServiceConnections.isEmpty())
    }

    interface ValueBinder {
        fun returnValue(value: Int): Int
    }

    interface FlowBinder {
        val flow: Flow<Int>
    }

    class ValueBinderService : Service(), ValueBinder {
        override fun returnValue(value: Int) = value

        override fun onBind(intent: Intent?) = null
    }

    class FlowBinderService : Service(), FlowBinder {
        override val flow = flowOf(1, 2, 3, 4, 5)
        override fun onBind(intent: Intent?) = null
    }
}
