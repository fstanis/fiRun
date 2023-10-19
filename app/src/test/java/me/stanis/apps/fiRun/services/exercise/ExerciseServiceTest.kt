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

package me.stanis.apps.fiRun.services.exercise

import android.os.Bundle
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.data.DataPoint
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseEndReason
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.ExerciseState as HealthExerciseState
import androidx.health.services.client.data.ExerciseStateInfo
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType as HealthExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.data.HeartRateAccuracy as HealthHRAccuracy
import androidx.health.services.client.data.SampleDataPoint
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.database.FakeDataStores
import me.stanis.apps.fiRun.database.datastore.CurrentExerciseData
import me.stanis.apps.fiRun.database.datastore.SettingsData
import me.stanis.apps.fiRun.models.ExerciseState
import me.stanis.apps.fiRun.models.HeartRate
import me.stanis.apps.fiRun.models.enums.ExerciseStatus
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.models.enums.HeartRateAccuracy
import me.stanis.apps.fiRun.models.enums.HeartRateSource
import me.stanis.apps.fiRun.modules.ClockModule
import me.stanis.apps.fiRun.modules.DataStoreModule
import me.stanis.apps.fiRun.modules.HealthModule
import me.stanis.apps.fiRun.util.binder.BinderConnectionHelper.createBinderConnection
import me.stanis.apps.fiRun.util.binder.ServiceBinderConnection
import me.stanis.apps.fiRun.util.clock.Clock
import me.stanis.apps.fiRun.util.errors.ServiceError
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.argThat
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@HiltAndroidTest
@Config(application = HiltTestApplication::class)
@UninstallModules(HealthModule::class, DataStoreModule::class, ClockModule::class)
@RunWith(RobolectricTestRunner::class)
class ExerciseServiceTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    val mockExerciseClient: ExerciseClient = mock()

    @BindValue
    val fakeSettings = FakeDataStores.create(
        SettingsData(
            deviceId = "",
            onlyHighHrAccuracy = false,
            keepScreenOn = false
        )
    )

    @BindValue
    val fakeData = FakeDataStores.create(CurrentExerciseData.EMPTY)

    @BindValue
    val fakeClock = object : Clock {
        override fun now(): Instant = Instant.ofEpochSecond(123)
        override fun currentTimeMillis(): Long = 0
        override fun elapsedRealtime(): Long = 123
    }

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var binder: ServiceBinderConnection<ExerciseService>
    private lateinit var mockCallback: ExerciseUpdateCallback

    @Before
    fun setUp() {
        hiltRule.inject()
        whenever(mockExerciseClient.getCurrentExerciseInfoAsync()).thenReturn(
            Futures.immediateFuture(
                ExerciseInfo(
                    ExerciseTrackedStatus.NO_EXERCISE_IN_PROGRESS,
                    HealthExerciseType.UNKNOWN
                )
            )
        )
        binder = createBinderConnection<ExerciseService>()
        val captor = argumentCaptor<ExerciseUpdateCallback>()
        verify(mockExerciseClient).setUpdateCallback(captor.capture())
        mockCallback = captor.firstValue
        mockCallback.onRegistered()
    }

    @After
    fun tearDown() {
        binder.unbind()
    }

    @Test
    fun `startExercise calls startExerciseAsync`() = testScope.runTest {
        val config = RunExerciseConfig.forType(ExerciseType.IndoorRun, true)
        whenever(
            mockExerciseClient.startExerciseAsync(
                argThat {
                    exerciseType == config.exerciseType && dataTypes == config.dataTypes
                }
            )
        ).thenReturn(Futures.immediateVoidFuture() as ListenableFuture<Void>)
        binder.runWhenConnected { it.startExercise(ExerciseType.IndoorRun, true) }
        runCurrent()
    }

    @Test
    fun `statusUpdates reflects status from callback`() = testScope.runTest {
        val statusUpdates = binder.flowWhenConnected(ExerciseBinder::stateUpdates)
            .shareIn(backgroundScope, SharingStarted.Eagerly, replay = 10)
        runCurrent()
        mockCallback.onExerciseUpdateReceived(createExerciseUpdate())
        runCurrent()
        assertEquals(
            listOf(
                ExerciseState(
                    status = ExerciseStatus.NotStarted,
                    lastUpdated = Duration.ofMillis(123),
                    startTime = null,
                    lastStateTransitionTime = null,
                    activeDuration = null
                ),
                ExerciseState(
                    status = ExerciseStatus.InProgress,
                    lastUpdated = Duration.ofSeconds(1234),
                    startTime = Instant.ofEpochSecond(321),
                    lastStateTransitionTime = Instant.ofEpochSecond(123),
                    activeDuration = Duration.ofSeconds(4321)
                )
            ),
            statusUpdates.replayCache
        )
    }

    @Test
    fun `heartRateUpdates reflects status from callback`() = testScope.runTest {
        val heartRateUpdates = binder.flowWhenConnected(ExerciseBinder::heartRateUpdates)
            .shareIn(backgroundScope, SharingStarted.Eagerly, replay = 10)
        runCurrent()
        mockCallback.onExerciseUpdateReceived(
            createExerciseUpdate(
                listOf(
                    SampleDataPoint(
                        DataType.HEART_RATE_BPM,
                        79.0,
                        Duration.ofSeconds(1),
                        Bundle.EMPTY,
                        HealthHRAccuracy(HealthHRAccuracy.SensorStatus.ACCURACY_HIGH)
                    ),
                    SampleDataPoint(
                        DataType.HEART_RATE_BPM,
                        80.0,
                        Duration.ofSeconds(2),
                        Bundle.EMPTY,
                        HealthHRAccuracy(HealthHRAccuracy.SensorStatus.ACCURACY_HIGH)
                    )
                )
            )
        )
        runCurrent()
        assertEquals(
            listOf(
                HeartRate(
                    bpm = 79,
                    accuracy = HeartRateAccuracy.High,
                    source = HeartRateSource.Internal,
                    instant = Instant.ofEpochMilli(1000 - 123),
                    exerciseDuration = Duration.ofSeconds(4321)
                ),
                HeartRate(
                    bpm = 80,
                    accuracy = HeartRateAccuracy.High,
                    source = HeartRateSource.Internal,
                    instant = Instant.ofEpochMilli(2000 - 123),
                    exerciseDuration = Duration.ofSeconds(4321)
                )
            ),
            heartRateUpdates.replayCache
        )
    }

    @Test
    fun `errors reflects ending in error`() = testScope.runTest {
        val errors = binder.flowWhenConnected(ExerciseBinder::errors)
            .shareIn(backgroundScope, SharingStarted.Eagerly, replay = 10)
        runCurrent()
        mockCallback.onExerciseUpdateReceived(
            createEndedExerciseUpdate(ExerciseEndReason.AUTO_END_MISSING_LISTENER)
        )
        runCurrent()
        assertEquals(
            listOf(ServiceError.WithMessage("Exercise prematurely ended")),
            errors.replayCache
        )
    }

    private fun createExerciseUpdate(dataPointList: List<DataPoint<*>> = emptyList()) =
        mock<ExerciseUpdate>().apply {
            whenever(exerciseStateInfo).thenReturn(
                ExerciseStateInfo(
                    HealthExerciseState.ACTIVE,
                    ExerciseEndReason.UNKNOWN
                )
            )
            whenever(activeDurationCheckpoint).thenReturn(
                ExerciseUpdate.ActiveDurationCheckpoint(
                    Instant.ofEpochSecond(123),
                    Duration.ofSeconds(4321)
                )
            )
            whenever(startTime).thenReturn(Instant.ofEpochSecond(321))
            whenever(getUpdateDurationFromBoot()).thenReturn(Duration.ofSeconds(1234))
            whenever(latestMetrics).thenReturn(DataPointContainer(dataPointList))
        }

    private fun createEndedExerciseUpdate(@ExerciseEndReason reason: Int) =
        mock<ExerciseUpdate>().apply {
            whenever(exerciseStateInfo).thenReturn(
                ExerciseStateInfo(
                    HealthExerciseState.ENDED,
                    reason
                )
            )
            whenever(activeDurationCheckpoint).thenReturn(
                ExerciseUpdate.ActiveDurationCheckpoint(
                    Instant.ofEpochSecond(123),
                    Duration.ofSeconds(4321)
                )
            )
            whenever(startTime).thenReturn(Instant.ofEpochSecond(321))
            whenever(getUpdateDurationFromBoot()).thenReturn(Duration.ofSeconds(1234))
            whenever(latestMetrics).thenReturn(DataPointContainer(emptyList()))
        }
}
