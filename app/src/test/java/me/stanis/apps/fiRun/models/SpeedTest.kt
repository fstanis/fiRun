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
class SpeedTest {
    @Test
    fun `fromExerciseSpeed with valid input returns valid pace`() {
        val clock = mock<Clock>()
        whenever(clock.currentTimeMillis()).thenReturn(500)
        whenever(clock.elapsedRealtime()).thenReturn(200)
        val underTest = Speed.fromExercise(
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
            Speed(
                current = 200.0,
                instant = Instant.ofEpochMilli(123_300),
                exerciseDuration = Duration.ofSeconds(123)
            ),
            underTest
        )
    }

    @Test
    fun `fromExerciseSpeed with invalid input returns null`() {
        assertNull(Speed.fromExercise(null, Duration.ofSeconds(123), mock()))
        assertNull(
            Speed.fromExercise(
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
}
