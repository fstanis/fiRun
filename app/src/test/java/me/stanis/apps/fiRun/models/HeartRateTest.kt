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
import androidx.health.services.client.data.HeartRateAccuracy as HealthHRAccuracy
import androidx.health.services.client.data.SampleDataPoint
import com.polar.sdk.api.model.PolarHrData.PolarHrSample
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import me.stanis.apps.fiRun.models.enums.HeartRateAccuracy
import me.stanis.apps.fiRun.models.enums.HeartRateSource
import me.stanis.apps.fiRun.util.clock.Clock
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HeartRateTest {
    @Test
    fun `fromPolarHrSample with valid input returns valid heart rate`() {
        val clock = mock<Clock>()
        whenever(clock.now()).thenReturn(Instant.ofEpochMilli(1234))
        val underTest = HeartRate.fromPolarHrSample(
            "deviceId",
            PolarHrSample(
                hr = 82,
                rrsMs = emptyList(),
                rrAvailable = false,
                contactStatus = true,
                contactStatusSupported = true
            ),
            clock
        )

        assertEquals(
            HeartRate(
                bpm = 82,
                accuracy = HeartRateAccuracy.Unknown,
                source = HeartRateSource.External("deviceId"),
                exerciseDuration = Duration.ZERO,
                instant = Instant.ofEpochMilli(1234)
            ),
            underTest
        )
    }

    @Test
    fun `fromPolarHrSample with no contact returns null`() {
        assertNull(
            HeartRate.fromPolarHrSample(
                "deviceId",
                PolarHrSample(
                    hr = 82,
                    rrsMs = emptyList(),
                    rrAvailable = false,
                    contactStatus = false,
                    contactStatusSupported = true
                ),
                mock()
            )
        )
    }

    @Test
    fun `fromExercise with valid input returns valid heart rate`() {
        val clock = mock<Clock>()
        whenever(clock.currentTimeMillis()).thenReturn(500)
        whenever(clock.elapsedRealtime()).thenReturn(200)
        val underTest = HeartRate.fromExercise(
            SampleDataPoint(
                dataType = DataType.HEART_RATE_BPM,
                value = 82.0,
                timeDurationFromBoot = Duration.ofSeconds(123),
                metadata = Bundle.EMPTY,
                accuracy = HealthHRAccuracy(HealthHRAccuracy.SensorStatus.ACCURACY_MEDIUM)
            ),
            Duration.ofSeconds(123),
            clock
        )

        assertEquals(
            HeartRate(
                bpm = 82,
                accuracy = HeartRateAccuracy.Medium,
                source = HeartRateSource.Internal,
                exerciseDuration = Duration.ofSeconds(123),
                instant = Instant.ofEpochMilli(123_300)
            ),
            underTest
        )
    }

    @Test
    fun `fromExercise without accuracy returns valid heart rate`() {
        val clock = mock<Clock>()
        whenever(clock.currentTimeMillis()).thenReturn(500)
        whenever(clock.elapsedRealtime()).thenReturn(200)
        val underTest = HeartRate.fromExercise(
            SampleDataPoint(
                dataType = DataType.HEART_RATE_BPM,
                value = 86.0,
                timeDurationFromBoot = Duration.ofSeconds(123),
                metadata = Bundle.EMPTY,
                accuracy = null
            ),
            Duration.ofSeconds(123),
            clock
        )

        assertEquals(
            HeartRate(
                bpm = 86,
                accuracy = HeartRateAccuracy.Unknown,
                source = HeartRateSource.Internal,
                exerciseDuration = Duration.ofSeconds(123),
                instant = Instant.ofEpochMilli(123_300)
            ),
            underTest
        )
    }

    @Test
    fun `fromExercise with invalid input returns null`() {
        assertNull(
            HeartRate.fromExercise(
                SampleDataPoint(
                    dataType = DataType.ABSOLUTE_ELEVATION,
                    value = 91.0,
                    timeDurationFromBoot = Duration.ofSeconds(123),
                    metadata = Bundle.EMPTY,
                    accuracy = null
                ),
                Duration.ofSeconds(123),
                mock()
            )
        )
        assertNull(
            HeartRate.fromExercise(
                SampleDataPoint(
                    dataType = DataType.HEART_RATE_BPM,
                    value = 91.0,
                    timeDurationFromBoot = Duration.ofSeconds(123),
                    metadata = Bundle.EMPTY,
                    accuracy = HealthHRAccuracy(HealthHRAccuracy.SensorStatus.NO_CONTACT)
                ),
                Duration.ofSeconds(123),
                mock()
            )
        )
        assertNull(
            HeartRate.fromExercise(
                SampleDataPoint(
                    dataType = DataType.HEART_RATE_BPM,
                    value = 91.0,
                    timeDurationFromBoot = Duration.ofSeconds(123),
                    metadata = Bundle.EMPTY,
                    accuracy = HealthHRAccuracy(HealthHRAccuracy.SensorStatus.UNRELIABLE)
                ),
                Duration.ofSeconds(123),
                mock()
            )
        )
    }
}
