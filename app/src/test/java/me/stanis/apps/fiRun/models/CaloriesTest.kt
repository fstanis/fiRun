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

import androidx.health.services.client.data.CumulativeDataPoint
import androidx.health.services.client.data.DataType
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class CaloriesTest {
    @Test
    fun `fromExercise should return Calories when sample is valid`() {
        val underTest = Calories.fromExercise(
            CumulativeDataPoint(
                dataType = DataType.CALORIES_TOTAL,
                total = 100.0,
                start = Instant.ofEpochSecond(1),
                end = Instant.ofEpochSecond(2)
            ),
            Duration.ofMinutes(30)
        )

        assertEquals(
            Calories(
                total = 100,
                exerciseDuration = Duration.ofMinutes(30),
                instant = Instant.ofEpochSecond(2)
            ),
            underTest
        )
    }

    @Test
    fun `fromExercise should return null when sample is invalid`() {
        assertNull(Calories.fromExercise(null, Duration.ofMinutes(30)))
        assertNull(
            Calories.fromExercise(
                CumulativeDataPoint(
                    dataType = DataType.DECLINE_DISTANCE_TOTAL,
                    total = 100.0,
                    start = Instant.ofEpochSecond(1),
                    end = Instant.ofEpochSecond(2)
                ),
                Duration.ofMinutes(30)
            )
        )
    }
}
