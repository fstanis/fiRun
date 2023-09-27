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
import androidx.health.services.client.data.HeartRateAccuracy as HealthHRAccuracy
import androidx.health.services.client.data.SampleDataPoint
import com.polar.sdk.api.model.PolarHrData.PolarHrSample
import java.time.Duration
import java.time.Instant
import kotlin.math.roundToInt
import me.stanis.apps.fiRun.models.enums.HeartRateAccuracy
import me.stanis.apps.fiRun.models.enums.HeartRateSource
import me.stanis.apps.fiRun.services.exercise.instant
import me.stanis.apps.fiRun.services.exercise.sensorStatus
import me.stanis.apps.fiRun.util.clock.Clock

data class HeartRate(
    val bpm: Int,
    val accuracy: HeartRateAccuracy,
    val source: HeartRateSource,
    override val instant: Instant,
    val exerciseDuration: Duration
) : DataWithInstant {
    companion object {
        fun fromPolarHrSample(deviceId: String, sample: PolarHrSample, clock: Clock): HeartRate? {
            if (sample.contactStatusSupported && !sample.contactStatus) {
                return null
            }
            return HeartRate(
                sample.hr,
                HeartRateAccuracy.Unknown,
                HeartRateSource.External(deviceId),
                instant = clock.now(),
                exerciseDuration = Duration.ZERO
            )
        }

        fun fromExercise(
            sample: SampleDataPoint<Double>,
            exerciseDuration: Duration,
            clock: Clock
        ): HeartRate? {
            if (!isSampleValid(sample)) {
                return null
            }
            return HeartRate(
                sample.value.roundToInt(),
                when (sample.sensorStatus) {
                    HealthHRAccuracy.SensorStatus.ACCURACY_LOW -> HeartRateAccuracy.Low
                    HealthHRAccuracy.SensorStatus.ACCURACY_MEDIUM -> HeartRateAccuracy.Medium
                    HealthHRAccuracy.SensorStatus.ACCURACY_HIGH -> HeartRateAccuracy.High
                    else -> HeartRateAccuracy.Unknown
                },
                HeartRateSource.Internal,
                instant = sample.instant(clock),
                exerciseDuration = exerciseDuration
            )
        }

        private fun isSampleValid(sample: SampleDataPoint<Double>) =
            sample.dataType == DataType.HEART_RATE_BPM &&
                sample.sensorStatus != HealthHRAccuracy.SensorStatus.NO_CONTACT &&
                sample.sensorStatus != HealthHRAccuracy.SensorStatus.UNRELIABLE
    }
}
