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

import androidx.health.services.client.data.ExerciseEndReason
import androidx.health.services.client.data.ExerciseInfo
import androidx.health.services.client.data.ExerciseState as HealthExerciseState
import androidx.health.services.client.data.ExerciseStateInfo
import androidx.health.services.client.data.ExerciseTrackedStatus
import androidx.health.services.client.data.ExerciseType
import androidx.health.services.client.data.ExerciseUpdate
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import me.stanis.apps.fiRun.models.enums.ExerciseStatus
import me.stanis.apps.fiRun.util.clock.Clock
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ExerciseStateTest {
    @Test
    fun `isPartial should return true when any of the properties is null`() {
        val state1 = ExerciseState(
            status = ExerciseStatus.InProgress,
            lastUpdated = Duration.ofMinutes(10),
            startTime = null,
            lastStateTransitionTime = Instant.ofEpochSecond(435),
            activeDuration = Duration.ofMinutes(5)
        )
        assertTrue(state1.isPartial)

        val state2 = ExerciseState(
            status = ExerciseStatus.InProgress,
            lastUpdated = Duration.ofMinutes(10),
            startTime = Instant.ofEpochSecond(234),
            lastStateTransitionTime = null,
            activeDuration = Duration.ofMinutes(5)
        )
        assertTrue(state2.isPartial)

        val state3 = ExerciseState(
            status = ExerciseStatus.InProgress,
            lastUpdated = Duration.ofMinutes(10),
            startTime = Instant.ofEpochSecond(234),
            lastStateTransitionTime = Instant.ofEpochSecond(435),
            activeDuration = null
        )
        assertTrue(state3.isPartial)
    }

    @Test
    fun `isPartial should return false when all properties are not null`() {
        val state = ExerciseState(
            status = ExerciseStatus.InProgress,
            lastUpdated = Duration.ofMinutes(10),
            startTime = Instant.ofEpochSecond(234),
            lastStateTransitionTime = Instant.ofEpochSecond(435),
            activeDuration = Duration.ofMinutes(5)
        )
        assertFalse(state.isPartial)
    }

    @Test
    fun `getDuration should return null when lastStateTransitionTime or activeDuration is null`() {
        val state1 = ExerciseState(
            status = ExerciseStatus.InProgress,
            lastUpdated = Duration.ofMinutes(10),
            startTime = Instant.ofEpochSecond(234),
            lastStateTransitionTime = null,
            activeDuration = Duration.ofMinutes(5)
        )
        assertNull(state1.getDuration(Instant.now()))

        val state2 = ExerciseState(
            status = ExerciseStatus.InProgress,
            lastUpdated = Duration.ofMinutes(10),
            startTime = null,
            lastStateTransitionTime = Instant.ofEpochSecond(435),
            activeDuration = null
        )
        assertNull(state2.getDuration(Instant.now()))
    }

    @Test
    fun `getDuration should return correct duration when status is InProgress`() {
        val lastStateTransitionTime = Instant.ofEpochSecond(123)
        val activeDuration = Duration.ofMinutes(5)
        val state = ExerciseState(
            status = ExerciseStatus.InProgress,
            lastUpdated = Duration.ofMinutes(10),
            startTime = Instant.ofEpochSecond(234),
            lastStateTransitionTime = lastStateTransitionTime,
            activeDuration = activeDuration
        )
        val now = Instant.ofEpochSecond(560)
        val expectedDuration = activeDuration + Duration.between(lastStateTransitionTime, now)
        assertEquals(expectedDuration, state.getDuration(now))
    }

    @Test
    fun `getDuration should return activeDuration when status is not InProgress`() {
        val state = ExerciseState(
            status = ExerciseStatus.Paused,
            lastUpdated = Duration.ofMinutes(10),
            startTime = Instant.ofEpochSecond(234),
            lastStateTransitionTime = Instant.ofEpochSecond(435),
            activeDuration = Duration.ofMinutes(5)
        )

        assertEquals(Duration.ofMinutes(5), state.getDuration(Instant.now()))
    }

    @Test
    fun `createWithStatus should create an ExerciseState with the specified status`() {
        val clock = mock<Clock>()
        whenever(clock.elapsedRealtime()).thenReturn(500)

        val state = ExerciseState.createWithStatus(ExerciseStatus.Loading, clock)

        assertEquals(ExerciseStatus.Loading, state.status)
        assertEquals(Duration.ofMillis(500), state.lastUpdated)
    }

    @Test
    fun `fromExerciseUpdate should create an ExerciseState from an ExerciseUpdate`() {
        val startTime = Instant.ofEpochSecond(1000)
        val lastStateTransitionTime = Instant.ofEpochSecond(50)
        val activeDuration = Duration.ofMinutes(5)
        val updateDurationFromBoot = Duration.ofSeconds(100)
        val exerciseUpdate = mock<ExerciseUpdate>()
        whenever(exerciseUpdate.exerciseStateInfo).thenReturn(
            ExerciseStateInfo(HealthExerciseState.ACTIVE, ExerciseEndReason.UNKNOWN)
        )
        whenever(exerciseUpdate.activeDurationCheckpoint).thenReturn(
            ExerciseUpdate.ActiveDurationCheckpoint(lastStateTransitionTime, activeDuration)
        )
        whenever(exerciseUpdate.startTime).thenReturn(startTime)
        whenever(exerciseUpdate.getUpdateDurationFromBoot()).thenReturn(updateDurationFromBoot)

        val state = ExerciseState.fromExerciseUpdate(exerciseUpdate)

        assertEquals(ExerciseStatus.InProgress, state.status)
        assertEquals(updateDurationFromBoot, state.lastUpdated)
        assertEquals(startTime, state.startTime)
        assertEquals(lastStateTransitionTime, state.lastStateTransitionTime)
        assertEquals(activeDuration, state.activeDuration)
    }

    @Test
    fun `fromExerciseInfo should create an ExerciseState from an ExerciseInfo`() {
        val clock = mock<Clock>()
        whenever(clock.elapsedRealtime()).thenReturn(501)
        val exerciseInfo =
            ExerciseInfo(ExerciseTrackedStatus.OWNED_EXERCISE_IN_PROGRESS, ExerciseType.RUNNING)

        val state = ExerciseState.fromExerciseInfo(exerciseInfo, clock)

        assertEquals(ExerciseStatus.InProgress, state.status)
        assertEquals(Duration.ofMillis(501), state.lastUpdated)
        assertNull(state.startTime)
        assertNull(state.lastStateTransitionTime)
        assertNull(state.activeDuration)
    }
}
