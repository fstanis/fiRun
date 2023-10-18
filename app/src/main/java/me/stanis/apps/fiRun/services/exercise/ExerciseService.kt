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

import android.app.Notification
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.HealthServicesException
import androidx.health.services.client.clearUpdateCallback
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCurrentExerciseInfo
import androidx.health.services.client.pauseExercise
import androidx.health.services.client.resumeExercise
import androidx.health.services.client.startExercise
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.stanis.apps.fiRun.database.datastore.SettingsData
import me.stanis.apps.fiRun.models.AveragePace
import me.stanis.apps.fiRun.models.Calories
import me.stanis.apps.fiRun.models.CurrentPace
import me.stanis.apps.fiRun.models.Distance
import me.stanis.apps.fiRun.models.ExerciseState
import me.stanis.apps.fiRun.models.HeartRate
import me.stanis.apps.fiRun.models.Speed
import me.stanis.apps.fiRun.models.enums.ExerciseStatus
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.models.enums.HeartRateAccuracy
import me.stanis.apps.fiRun.persistence.ExerciseWriter
import me.stanis.apps.fiRun.services.exercise.ExerciseCallback.Companion.setExerciseCallback
import me.stanis.apps.fiRun.util.binder.BindableForegroundService
import me.stanis.apps.fiRun.util.clock.Clock
import me.stanis.apps.fiRun.util.errors.ServiceError
import me.stanis.apps.fiRun.util.notifications.Notifications

@AndroidEntryPoint
class ExerciseService : BindableForegroundService(), ExerciseBinder {
    @Inject
    lateinit var exerciseClient: ExerciseClient

    @Inject
    lateinit var notifications: Notifications

    @Inject
    lateinit var exerciseWriter: ExerciseWriter

    @Inject
    lateinit var settings: DataStore<SettingsData>

    @Inject
    lateinit var clock: Clock

    private lateinit var onlyHighHrAccuracy: StateFlow<Boolean>

    override val stateUpdates =
        MutableSharedFlow<ExerciseState>(replay = 1, extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    override val heartRateUpdates =
        MutableSharedFlow<HeartRate>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    override val distanceUpdates =
        MutableSharedFlow<Distance>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    override val caloriesUpdates =
        MutableSharedFlow<Calories>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    override val currentPaceUpdates =
        MutableSharedFlow<CurrentPace>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    override val averagePaceUpdates =
        MutableSharedFlow<AveragePace>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    private val speedUpdates =
        MutableSharedFlow<Speed>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    override val errors = MutableSharedFlow<ServiceError>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    private val statusUpdates = stateUpdates.map { it.status }.distinctUntilChanged()

    override val notification: Notification
        get() = notifications.createExerciseNotification("Exercise in progress")

    override suspend fun startExercise(type: ExerciseType, includeHeartRate: Boolean) {
        clearFlows()
        exerciseWriter.create(type)
        exerciseClient.tryInvoke {
            startExercise(RunExerciseConfig.forType(type, includeHeartRate))
        }
    }

    override suspend fun endExercise() {
        exerciseClient.tryInvoke(ExerciseClient::endExercise)
    }

    override suspend fun resetExercise() {
        stateUpdates.emit(ExerciseState.createWithStatus(ExerciseStatus.NotStarted, clock))
    }

    override suspend fun pauseExercise() {
        exerciseClient.tryInvoke(ExerciseClient::pauseExercise)
    }

    override suspend fun resumeExercise() {
        exerciseClient.tryInvoke(ExerciseClient::resumeExercise)
    }

    private fun clearFlows() {
        stateUpdates.resetReplayCache()
        heartRateUpdates.resetReplayCache()
        distanceUpdates.resetReplayCache()
        caloriesUpdates.resetReplayCache()
        currentPaceUpdates.resetReplayCache()
        averagePaceUpdates.resetReplayCache()
        speedUpdates.resetReplayCache()
    }

    private suspend fun processExerciseUpdate(update: ExerciseUpdate) {
        val state = ExerciseState.fromExerciseUpdate(update)
        stateUpdates.emit(state)
        val duration = state.getDuration(clock.now())
        if (duration != null) {
            update.heartRateSamples
                .mapNotNull { HeartRate.fromExercise(it, duration, clock) }
                .filter { !onlyHighHrAccuracy.value || it.accuracy == HeartRateAccuracy.High }
                .forEach {
                    heartRateUpdates.emit(it)
                }
            update.speedSamples
                .forEach {
                    val currentPace = CurrentPace.fromExerciseSpeed(it, duration, clock)
                    val speed = Speed.fromExercise(it, duration, clock)
                    if (currentPace != null) {
                        currentPaceUpdates.emit(currentPace)
                    }
                    if (speed != null) {
                        speedUpdates.emit(speed)
                    }
                }
            Distance.fromExercise(update.totalDistance, duration)
                ?.let { distanceUpdates.emit(it) }
            Calories.fromExercise(update.totalCalories, duration)
                ?.let { caloriesUpdates.emit(it) }
            AveragePace.fromExerciseSpeed(update.speedStats, duration)
                ?.let { averagePaceUpdates.emit(it) }
        } else {
            Log.e("ExerciseService", "Received update with no duration")
        }
        if (update.isEndedInError) {
            errors.emit(ServiceError.WithMessage("Exercise prematurely ended"))
        }
    }

    private fun processStatusChange(exerciseStatus: ExerciseStatus) {
        shouldForeground = exerciseStatus == ExerciseStatus.InProgress
    }

    private suspend fun startWriters() = coroutineScope {
        launch { exerciseWriter.writeStateUpdates(stateUpdates) }
        launch { exerciseWriter.writeHeartRateUpdates(heartRateUpdates) }
        launch { exerciseWriter.writeDistanceUpdates(distanceUpdates) }
        launch { exerciseWriter.writeSpeedUpdates(speedUpdates) }
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch {
            onlyHighHrAccuracy = settings.data.map { it.onlyHighHrAccuracy }.stateIn(this)
            val exerciseInfo = exerciseClient.getCurrentExerciseInfo()
            stateUpdates.emit(ExerciseState.fromExerciseInfo(exerciseInfo, clock))
            val exerciseCallback = exerciseClient.setExerciseCallback()
            try {
                exerciseCallback.exerciseUpdates.onEach(::processExerciseUpdate).launchIn(this)
                statusUpdates.onEach(::processStatusChange).launchIn(this)
                startWriters()
                awaitCancellation()
            } finally {
                exerciseClient.clearUpdateCallback(exerciseCallback)
            }
        }
    }

    private suspend fun ExerciseClient.tryInvoke(block: suspend ExerciseClient.() -> Unit) {
        try {
            block()
        } catch (exception: HealthServicesException) {
            errors.tryEmit(ServiceError.WithThrowable(exception))
            if (!try {
                exerciseClient.getCurrentExerciseInfo().isActive
            } catch (_: HealthServicesException) {
                    false
                }
            ) {
                resetExercise()
            }
        }
    }

    companion object {
        private const val EXTRA_BUFFER_CAPACITY = 64
    }
}
