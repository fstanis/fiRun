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

package me.stanis.apps.fiRun.util.heartrate

import androidx.health.services.client.data.HeartRateAccuracy
import androidx.health.services.client.data.SampleDataPoint
import com.polar.sdk.api.model.PolarHrData
import kotlin.math.roundToInt

data class HeartRate(
    val heartRate: Int,
    val source: Source,
    val currentTimeMillis: Long = System.currentTimeMillis()
) {
    enum class Source {
        Internal,
        InternalHighAccuracy,
        External,
    }

    companion object {
        fun fromPolarHrData(data: PolarHrData): HeartRate? {
            val samples = data.samples.filter { !it.contactStatusSupported || it.contactStatus }
            if (samples.isEmpty()) {
                return null
            }
            return HeartRate(samples.last().hr, Source.External)
        }

        fun fromExercise(samples: List<SampleDataPoint<Double>>): HeartRate? {
            val accurateSamples =
                samples.filter { (it.accuracy as? HeartRateAccuracy)?.sensorStatus == HeartRateAccuracy.SensorStatus.ACCURACY_HIGH }
            if (accurateSamples.isNotEmpty()) {
                return HeartRate(
                    accurateSamples.last().value.roundToInt(),
                    Source.InternalHighAccuracy
                )
            } else if (samples.isNotEmpty()) {
                return HeartRate(
                    samples.last().value.roundToInt(),
                    Source.Internal
                )
            }
            return null
        }
    }
}
