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

package me.stanis.apps.fiRun.services.exercise

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import androidx.health.services.client.clearUpdateCallback
import androidx.health.services.client.data.ExerciseUpdate
import androidx.health.services.client.endExercise
import androidx.health.services.client.startExercise
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.stanis.apps.fiRun.util.heartrate.HeartRate
import javax.inject.Inject

@AndroidEntryPoint
class ExerciseService : LifecycleService() {
    @Inject
    lateinit var exerciseClient: ExerciseClient

    private val mutableStatus = MutableStateFlow(ExerciseStatus())
    private val mutableHeartRate = MutableStateFlow<HeartRate?>(null)

    private val binder = object : Binder(), ExerciseBinder {
        override fun startRun(type: ExerciseBinder.RunType, includeHeartRate: Boolean) =
            this@ExerciseService.startRun(type, includeHeartRate)

        override fun endRun() = this@ExerciseService.endRun()
        override fun reset() = this@ExerciseService.reset()
        override val status: StateFlow<ExerciseStatus> get() = mutableStatus
        override val lastHeartRate: StateFlow<HeartRate?> get() = mutableHeartRate
    }

    private var exerciseCallback: ExerciseUpdateCallback? = null

    private fun startRun(type: ExerciseBinder.RunType, includeHeartRate: Boolean) {
        lifecycleScope.launch {
            exerciseClient.startExercise(RunExerciseConfig.forType(type, includeHeartRate))
        }
    }

    private fun endRun() {
        lifecycleScope.launch {
            exerciseClient.endExercise()
        }
    }

    private fun reset() {
        mutableStatus.value = ExerciseStatus()
    }

    private fun onExerciseUpdateReceived(update: ExerciseUpdate) {
        val duration = update.duration ?: return
        val currentInfo = mutableStatus.value.exerciseInfo
        HeartRate.fromExercise(update.heartRateSamples)?.let { mutableHeartRate.value = it }
        val exerciseInfo = ExerciseStatus.ExerciseInfo(
            duration = duration,
            totalDistance = update.totalDistance ?: currentInfo.totalDistance,
            currentPace = update.currentPace ?: currentInfo.currentPace,
            averagePace = update.averagePace ?: currentInfo.averagePace
        )
        mutableStatus.value = ExerciseStatus(
            state = when {
                update.isActive -> ExerciseStatus.ExerciseState.InProgress
                update.isLoading -> ExerciseStatus.ExerciseState.Loading
                update.isEnded -> ExerciseStatus.ExerciseState.Ended
                update.isPaused -> ExerciseStatus.ExerciseState.Paused
                else -> ExerciseStatus.ExerciseState.NotStarted
            },
            lastUpdated = update.getUpdateDurationFromBoot(),
            exerciseInfo = exerciseInfo
        )
    }

    override fun onCreate() {
        super.onCreate()
        lifecycleScope.launch {
            exerciseCallback = exerciseClient.setExerciseUpdateCallback(::onExerciseUpdateReceived)
            if (exerciseClient.isOtherAppInProgress()) {
                mutableStatus.update { it.copy(state = ExerciseStatus.ExerciseState.UsedByDifferentApp) }
            }
        }
    }

    override fun onDestroy() {
        lifecycleScope.launch(NonCancellable) {
            exerciseCallback?.let { exerciseClient.clearUpdateCallback(it) }
        }
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)

        return result
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }
}
