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

import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.StatisticalDataPoint
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class AveragePaceTest {
    @Test
    fun `fromExerciseSpeed with valid input returns valid pace`() {
        val underTest = AveragePace.fromExerciseSpeed(
            StatisticalDataPoint(
                dataType = DataType.SPEED_STATS,
                min = 12.0,
                max = 600.0,
                average = 200.0,
                start = Instant.ofEpochSecond(111),
                end = Instant.ofEpochSecond(222)
            ),
            Duration.ofSeconds(123)
        )

        assertEquals(
            AveragePace(
                average = Duration.ofMillis(5000),
                instant = Instant.ofEpochSecond(222),
                exerciseDuration = Duration.ofSeconds(123),
                isDerived = false
            ),
            underTest
        )
    }

    @Test
    fun `fromExerciseSpeed with invalid input returns null`() {
        assertNull(AveragePace.fromExerciseSpeed(null, Duration.ofSeconds(123)))
        assertNull(
            AveragePace.fromExerciseSpeed(
                StatisticalDataPoint(
                    dataType = DataType.ABSOLUTE_ELEVATION_STATS,
                    min = 12.0,
                    max = 600.0,
                    average = 134.0,
                    start = Instant.ofEpochSecond(1),
                    end = Instant.ofEpochSecond(2)
                ),
                Duration.ofSeconds(123)
            )
        )
    }

    @Test
    fun `fromExercisePace with valid input returns valid pace`() {
        val underTest = AveragePace.fromExercisePace(
            StatisticalDataPoint(
                dataType = DataType.PACE_STATS,
                min = 12.0,
                max = 600.0,
                average = 200.0,
                start = Instant.ofEpochSecond(111),
                end = Instant.ofEpochSecond(222)
            ),
            Duration.ofSeconds(123)
        )

        assertEquals(
            AveragePace(
                average = Duration.ofMillis(200),
                instant = Instant.ofEpochSecond(222),
                exerciseDuration = Duration.ofSeconds(123),
                isDerived = false
            ),
            underTest
        )
    }

    @Test
    fun `fromExercisePace with invalid input returns null`() {
        assertNull(AveragePace.fromExercisePace(null, Duration.ofSeconds(123)))
        assertNull(
            AveragePace.fromExercisePace(
                StatisticalDataPoint(
                    dataType = DataType.ABSOLUTE_ELEVATION_STATS,
                    min = 12.0,
                    max = 600.0,
                    average = 134.0,
                    start = Instant.ofEpochSecond(1),
                    end = Instant.ofEpochSecond(2)
                ),
                Duration.ofSeconds(123)
            )
        )
    }

    @Test
    fun `fromDistance with valid input returns valid pace`() {
        val underTest = AveragePace.fromDistance(
            Distance(
                total = 20.0,
                instant = Instant.ofEpochSecond(111),
                exerciseDuration = Duration.ofMillis(1000)
            )
        )

        assertEquals(
            AveragePace(
                average = Duration.ofSeconds(50),
                instant = Instant.ofEpochSecond(111),
                exerciseDuration = Duration.ofMillis(1000),
                isDerived = true
            ),
            underTest
        )
    }
}
