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
import android.os.Binder
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.HealthServicesException
import androidx.health.services.client.clearUpdateCallback
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.endExercise
import androidx.health.services.client.getCurrentExerciseInfo
import androidx.health.services.client.pauseExercise
import androidx.health.services.client.resumeExercise
import androidx.health.services.client.startExercise
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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
import me.stanis.apps.fiRun.services.ForegroundableService
import me.stanis.apps.fiRun.services.exercise.ExerciseCallback.Companion.setExerciseUpdateCallback
import me.stanis.apps.fiRun.util.clock.Clock
import me.stanis.apps.fiRun.util.errors.ServiceError
import me.stanis.apps.fiRun.util.notifications.Notifications
import javax.inject.Inject

@AndroidEntryPoint
class ExerciseService : ForegroundableService<ExerciseBinder>() {
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

    private val mutableStatus =
        MutableSharedFlow<ExerciseState>(replay = 1, extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    private val mutableHeartRate =
        MutableSharedFlow<HeartRate>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    private val mutableDistance =
        MutableSharedFlow<Distance>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    private val mutableCalories =
        MutableSharedFlow<Calories>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    private val mutableCurrentPace =
        MutableSharedFlow<CurrentPace>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    private val mutableAveragePace =
        MutableSharedFlow<AveragePace>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    private val mutableSpeed =
        MutableSharedFlow<Speed>(extraBufferCapacity = EXTRA_BUFFER_CAPACITY)
    private val errors = MutableSharedFlow<ServiceError>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    override val notification: Notification
        get() = notifications.createExerciseNotification("Exercise in progress")

    override fun onCoroutineException(throwable: Throwable) {
        errors.tryEmit(ServiceError.WithThrowable(throwable))
    }

    override val binder: ExerciseBinder = object : Binder(), ExerciseBinder {
        override suspend fun startExercise(type: ExerciseType, includeHeartRate: Boolean) =
            this@ExerciseService.startExercise(type, includeHeartRate)

        override suspend fun endExercise() = this@ExerciseService.endExercise()
        override suspend fun resetExercise() = this@ExerciseService.resetExercise()
        override suspend fun pauseExercise() = this@ExerciseService.pauseExercise()
        override suspend fun resumeExercise() = this@ExerciseService.resumeExercise()
        override val stateUpdates: SharedFlow<ExerciseState> get() = mutableStatus
        override val heartRateUpdates: SharedFlow<HeartRate> get() = mutableHeartRate
        override val distanceUpdates: SharedFlow<Distance> get() = mutableDistance
        override val caloriesUpdates: SharedFlow<Calories> get() = mutableCalories
        override val currentPaceUpdates: SharedFlow<CurrentPace> get() = mutableCurrentPace
        override val averagePaceUpdates: SharedFlow<AveragePace> get() = mutableAveragePace
        override val errors: SharedFlow<ServiceError> get() = this@ExerciseService.errors
    }

    private var exerciseCallback: ExerciseUpdateCallback? = null

    private suspend fun startExercise(type: ExerciseType, includeHeartRate: Boolean) {
        clearFlows()
        exerciseWriter.create(type)
        exerciseClient.tryInvoke {
            startExercise(RunExerciseConfig.forType(type, includeHeartRate))
        }
    }

    private suspend fun endExercise() {
        exerciseClient.tryInvoke(ExerciseClient::endExercise)
    }

    private suspend fun resetExercise() {
        mutableStatus.emit(ExerciseState.createWithStatus(ExerciseStatus.NotStarted, clock))
    }

    private suspend fun pauseExercise() {
        exerciseClient.tryInvoke(ExerciseClient::pauseExercise)
    }

    private suspend fun resumeExercise() {
        exerciseClient.tryInvoke(ExerciseClient::resumeExercise)
    }

    private fun clearFlows() {
        mutableStatus.resetReplayCache()
        mutableHeartRate.resetReplayCache()
        mutableDistance.resetReplayCache()
        mutableCalories.resetReplayCache()
        mutableCurrentPace.resetReplayCache()
        mutableAveragePace.resetReplayCache()
        mutableSpeed.resetReplayCache()
    }

    private fun onExerciseUpdateReceived(update: ExerciseUpdate) {
        val status = ExerciseState.fromExerciseUpdate(update)
        val duration = status.getDuration(clock.now())
        mutableStatus.emitOrLog(status)
        if (duration != null) {
            update.heartRateSamples
                .mapNotNull { HeartRate.fromExercise(it, duration, clock) }
                .filter { !onlyHighHrAccuracy.value || it.accuracy == HeartRateAccuracy.High }
                .forEach {
                    mutableHeartRate.emitOrLog(it)
                }
            update.speedSamples
                .forEach {
                    val currentPace = CurrentPace.fromExerciseSpeed(it, duration, clock)
                    val speed = Speed.fromExercise(it, duration, clock)
                    if (currentPace != null) {
                        mutableCurrentPace.emitOrLog(currentPace)
                    }
                    if (speed != null) {
                        mutableSpeed.emitOrLog(speed)
                    }
                }
            Distance.fromExercise(update.totalDistance, duration)
                ?.let { mutableDistance.emitOrLog(it) }
            Calories.fromExercise(update.totalCalories, duration)
                ?.let { mutableCalories.emitOrLog(it) }
            AveragePace.fromExerciseSpeed(update.speedStats, duration)
                ?.let { mutableAveragePace.emitOrLog(it) }
        } else {
            Log.e("ExerciseService", "Received update with no duration")
        }
        if (update.isEndedInError) {
            errors.emitOrLog(ServiceError.WithMessage("Exercise prematurely ended"))
        }
    }

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            onlyHighHrAccuracy = settings.data.map { it.onlyHighHrAccuracy }.stateIn(this)
            val exerciseInfo = exerciseClient.getCurrentExerciseInfo()
            mutableStatus.emitOrLog(ExerciseState.fromExerciseInfo(exerciseInfo, clock))
            exerciseCallback = exerciseClient.setExerciseUpdateCallback(::onExerciseUpdateReceived)
            launch {
                mutableStatus.collect {
                    Log.i("ExerciseService", it.toString())
                }
            }
            val exerciseState = mutableStatus.map { it.status }.stateIn(this)
            launch {
                exerciseState.collect {
                    if (it == ExerciseStatus.InProgress) {
                        moveToForeground()
                    } else {
                        removeFromForeground()
                    }
                }
            }
            launch { exerciseWriter.writeStateUpdates(mutableStatus) }
            launch { exerciseWriter.writeHeartRateUpdates(mutableHeartRate) }
            launch { exerciseWriter.writeDistanceUpdates(mutableDistance) }
            launch { exerciseWriter.writeSpeedUpdates(mutableSpeed) }
        }
    }

    override fun onDestroy() {
        scope.launch(NonCancellable) {
            exerciseCallback?.let { exerciseClient.clearUpdateCallback(it) }
        }
        super.onDestroy()
    }

    private fun <T> MutableSharedFlow<T>.emitOrLog(value: T) {
        if (!tryEmit(value)) {
            Log.e("ExerciseService", "Failed to emit update")
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
