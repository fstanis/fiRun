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

package me.stanis.apps.fiRun.persistence

import androidx.datastore.core.DataStore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import me.stanis.apps.fiRun.database.dao.DistanceDao
import me.stanis.apps.fiRun.database.dao.ExerciseDao
import me.stanis.apps.fiRun.database.dao.HeartRateDao
import me.stanis.apps.fiRun.database.dao.SpeedDao
import me.stanis.apps.fiRun.database.datastore.CurrentExerciseData
import me.stanis.apps.fiRun.database.entities.DistanceEntity
import me.stanis.apps.fiRun.database.entities.ExerciseEntityCreate
import me.stanis.apps.fiRun.database.entities.ExerciseEntityUpdateEnd
import me.stanis.apps.fiRun.database.entities.ExerciseEntityUpdateStart
import me.stanis.apps.fiRun.database.entities.HeartRateEntity
import me.stanis.apps.fiRun.database.entities.SpeedEntity
import me.stanis.apps.fiRun.database.entities.helpers.ZonedDateTimeEntity
import me.stanis.apps.fiRun.models.Distance
import me.stanis.apps.fiRun.models.ExerciseState
import me.stanis.apps.fiRun.models.HeartRate
import me.stanis.apps.fiRun.models.Speed
import me.stanis.apps.fiRun.models.enums.ExerciseStatus
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.util.clock.Clock

@Singleton
class ExerciseWriter @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val distanceDao: DistanceDao,
    private val heartRateDao: HeartRateDao,
    private val speedDao: SpeedDao,
    private val currentExerciseData: DataStore<CurrentExerciseData>,
    private val clock: Clock
) {
    private val exerciseIdFlow = currentExerciseData.data.map { it.id }.distinctUntilChanged()

    suspend fun writeStateUpdates(statusUpdates: SharedFlow<ExerciseState>): Nothing =
        coroutineScope {
            val exerciseIdState = exerciseIdFlow.stateIn(this)
            statusUpdates.collect { state ->
                val exerciseId = exerciseIdState.value ?: return@collect
                if (!state.status.isActive) {
                    clearCurrent()
                }
                if (state.isPartial) {
                    return@collect
                }
                currentExerciseData.updateData {
                    it.copy(
                        inProgress = state.status == ExerciseStatus.InProgress,
                        lastStateTransitionTime = state.lastStateTransitionTime,
                        activeDuration = state.activeDuration
                    )
                }
                if (state.status == ExerciseStatus.InProgress) {
                    exerciseDao.updateStart(
                        ExerciseEntityUpdateStart(
                            exerciseId = exerciseId,
                            startTime = ZonedDateTimeEntity.from(state.startTime),
                            duration = state.getDuration(clock.now())
                        )
                    )
                } else if (state.status == ExerciseStatus.Ended) {
                    exerciseDao.updateEnd(
                        ExerciseEntityUpdateEnd(
                            exerciseId = exerciseId,
                            endTime = ZonedDateTimeEntity.from(state.lastStateTransitionTime),
                            duration = state.getDuration(clock.now())
                        )
                    )
                }
            }
        }

    suspend fun writeDistanceUpdates(distanceUpdates: SharedFlow<Distance>): Nothing =
        coroutineScope {
            val exerciseIdState = exerciseIdFlow.stateIn(this)
            distanceUpdates.collect { distance ->
                val exerciseId = exerciseIdState.value ?: return@collect
                distanceDao.insert(
                    DistanceEntity(
                        exerciseId = exerciseId,
                        distance = distance.total,
                        instant = distance.instant,
                        exerciseDuration = distance.exerciseDuration
                    )
                )
            }
        }

    suspend fun writeSpeedUpdates(speedUpdates: SharedFlow<Speed>): Nothing =
        coroutineScope {
            val exerciseIdState = exerciseIdFlow.stateIn(this)
            speedUpdates.collect { speed ->
                val exerciseId = exerciseIdState.value ?: return@collect
                speedDao.insert(
                    SpeedEntity(
                        exerciseId = exerciseId,
                        speed = speed.current,
                        instant = speed.instant,
                        exerciseDuration = speed.exerciseDuration
                    )
                )
            }
        }

    suspend fun writeHeartRateUpdates(heartRateUpdates: SharedFlow<HeartRate>): Nothing =
        coroutineScope {
            val currentExerciseDataState = currentExerciseData.data.stateIn(this)
            heartRateUpdates.collect { heartRate ->
                val exerciseId = currentExerciseDataState.value.id ?: return@collect
                var exerciseDuration = heartRate.exerciseDuration
                if (exerciseDuration.isZero) {
                    val duration = currentExerciseDataState.value.getDuration(heartRate.instant)
                        ?: return@collect
                    exerciseDuration = duration
                }
                heartRateDao.insert(
                    HeartRateEntity(
                        exerciseId = exerciseId,
                        bpm = heartRate.bpm,
                        accuracy = heartRate.accuracy,
                        source = heartRate.source,
                        instant = heartRate.instant,
                        exerciseDuration = exerciseDuration
                    )
                )
            }
        }

    suspend fun create(type: ExerciseType) {
        val exerciseId = UUID.randomUUID()
        exerciseDao.create(
            ExerciseEntityCreate(
                exerciseId = exerciseId,
                type = type
            )
        )
        setCurrent(exerciseId)
    }

    private suspend fun setCurrent(id: UUID) {
        currentExerciseData.updateData {
            CurrentExerciseData(
                id = id,
                inProgress = false,
                lastStateTransitionTime = null,
                activeDuration = null
            )
        }
    }

    private suspend fun clearCurrent() {
        currentExerciseData.updateData {
            CurrentExerciseData.EMPTY
        }
    }
}
