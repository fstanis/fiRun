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

import android.os.Bundle
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.SampleDataPoint
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import me.stanis.apps.fiRun.util.clock.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CurrentPaceTest {
    @Test
    fun `fromExerciseSpeed with valid input returns valid pace`() {
        val clock = mock<Clock>()
        whenever(clock.currentTimeMillis()).thenReturn(500)
        whenever(clock.elapsedRealtime()).thenReturn(200)
        val underTest = CurrentPace.fromExerciseSpeed(
            SampleDataPoint(
                dataType = DataType.SPEED,
                value = 200.0,
                timeDurationFromBoot = Duration.ofSeconds(123),
                metadata = Bundle.EMPTY,
                accuracy = null
            ),
            Duration.ofSeconds(123),
            clock
        )

        assertEquals(
            CurrentPace(
                current = Duration.ofMillis(5000),
                instant = Instant.ofEpochMilli(123_300),
                exerciseDuration = Duration.ofSeconds(123),
                isDerived = false
            ),
            underTest
        )
    }

    @Test
    fun `fromExerciseSpeed with invalid input returns null`() {
        assertNull(CurrentPace.fromExerciseSpeed(null, Duration.ofSeconds(123), mock()))
        assertNull(
            CurrentPace.fromExerciseSpeed(
                SampleDataPoint(
                    dataType = DataType.ABSOLUTE_ELEVATION,
                    value = 134.0,
                    timeDurationFromBoot = Duration.ofSeconds(123),
                    metadata = Bundle.EMPTY,
                    accuracy = null
                ),
                Duration.ofSeconds(3232),
                mock()
            )
        )
    }

    @Test
    fun `fromExercisePace with valid input returns valid pace`() {
        val clock = mock<Clock>()
        whenever(clock.currentTimeMillis()).thenReturn(500)
        whenever(clock.elapsedRealtime()).thenReturn(200)
        val underTest = CurrentPace.fromExercisePace(
            SampleDataPoint(
                dataType = DataType.PACE,
                value = 200.0,
                timeDurationFromBoot = Duration.ofSeconds(123),
                metadata = Bundle.EMPTY,
                accuracy = null
            ),
            Duration.ofSeconds(123),
            clock
        )

        assertEquals(
            CurrentPace(
                current = Duration.ofMillis(200),
                instant = Instant.ofEpochMilli(123_300),
                exerciseDuration = Duration.ofSeconds(123),
                isDerived = false
            ),
            underTest
        )
    }

    @Test
    fun `fromExercisePace with invalid input returns null`() {
        assertNull(CurrentPace.fromExercisePace(null, Duration.ofSeconds(123), mock()))
        assertNull(
            CurrentPace.fromExercisePace(
                SampleDataPoint(
                    dataType = DataType.ABSOLUTE_ELEVATION,
                    value = 134.0,
                    timeDurationFromBoot = Duration.ofSeconds(123),
                    metadata = Bundle.EMPTY,
                    accuracy = null
                ),
                Duration.ofSeconds(123),
                mock()
            )
        )
    }

    @Test
    fun `betweenDistances with valid input returns valid pace`() {
        val distance1 = Distance(
            total = 500.0,
            exerciseDuration = Duration.ofSeconds(10),
            instant = Instant.ofEpochSecond(15)
        )
        val distance2 = Distance(
            total = 600.0,
            exerciseDuration = Duration.ofSeconds(20),
            instant = Instant.ofEpochSecond(25)
        )
        val underTest = CurrentPace.betweenDistances(distance1, distance2)

        assertEquals(
            CurrentPace(
                current = Duration.ofSeconds(100),
                instant = Instant.ofEpochSecond(25),
                exerciseDuration = Duration.ofSeconds(20),
                isDerived = true
            ),
            underTest
        )
    }
}
