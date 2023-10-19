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

package me.stanis.apps.fiRun

import android.content.Context
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServices
import androidx.health.services.client.data.DataPoint
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.ExerciseState
import androidx.health.services.client.data.ExerciseUpdate
import com.google.common.util.concurrent.ListenableFuture
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration as KtDuration
import kotlin.time.toJavaDuration
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update

class ExerciseClientDelegate private constructor(private val exerciseClient: ExerciseClient) :
    ExerciseClient by exerciseClient {
    val updates = MutableSharedFlow<ExerciseUpdate>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val durationFlow = updates.mapNotNull { it.activeDurationCheckpoint }
        .map { it.activeDuration + Duration.between(it.time, Instant.now()) }.conflate()
    private val stateInfoFlow = updates
        .map { it.exerciseStateInfo.state }
        .distinctUntilChanged()
        .conflate()

    private val dataPoints = MutableStateFlow(emptyList<DataPoint<*>>())
    val dataPointContainer = dataPoints.map { DataPointContainer(it) }

    private var callbackDelegate: ExerciseUpdateCallback? = null
    private var originalCallback: ExerciseUpdateCallback? = null

    override fun setUpdateCallback(callback: ExerciseUpdateCallback) {
        originalCallback = callback
        callbackDelegate = object : ExerciseUpdateCallback by callback {
            override fun onExerciseUpdateReceived(update: ExerciseUpdate) {
                callback.onExerciseUpdateReceived(update)
                updates.tryEmit(update)
                dataPoints.update { prev ->
                    prev.toMutableList().also {
                        it.addAll(update.latestMetrics.sampleDataPoints)
                        it.addAll(update.latestMetrics.intervalDataPoints)
                        it.addAll(update.latestMetrics.cumulativeDataPoints)
                        it.addAll(update.latestMetrics.statisticalDataPoints)
                    }
                }
            }
        }.also { exerciseClient.setUpdateCallback(it) }
    }

    override fun clearUpdateCallbackAsync(
        callback: ExerciseUpdateCallback
    ): ListenableFuture<Void> {
        assertEquals(originalCallback, callback)
        assertNotNull(callbackDelegate)
        return exerciseClient.clearUpdateCallbackAsync(callbackDelegate!!)
    }

    fun clearDataPoints() {
        dataPoints.value = emptyList()
    }

    suspend fun awaitExerciseDuration(duration: KtDuration) {
        durationFlow.first { it >= duration.toJavaDuration() }
    }

    suspend fun awaitExerciseState(exerciseState: ExerciseState) {
        stateInfoFlow.first { it == exerciseState }
    }

    companion object {
        fun from(context: Context) =
            ExerciseClientDelegate(HealthServices.getClient(context).exerciseClient)
    }
}
