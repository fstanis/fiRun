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

package me.stanis.apps.fiRun.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.database.Db
import me.stanis.apps.fiRun.database.FakeDataStores
import me.stanis.apps.fiRun.database.dao.DistanceDao
import me.stanis.apps.fiRun.database.dao.ExerciseDao
import me.stanis.apps.fiRun.database.dao.HeartRateDao
import me.stanis.apps.fiRun.database.dao.SpeedDao
import me.stanis.apps.fiRun.database.datastore.CurrentExerciseData
import me.stanis.apps.fiRun.database.entities.DistanceEntity
import me.stanis.apps.fiRun.database.entities.ExerciseEntity
import me.stanis.apps.fiRun.database.entities.ExerciseEntityCreate
import me.stanis.apps.fiRun.database.entities.HeartRateEntity
import me.stanis.apps.fiRun.database.entities.SpeedEntity
import me.stanis.apps.fiRun.database.entities.helpers.ZonedDateTimeEntity
import me.stanis.apps.fiRun.models.Distance
import me.stanis.apps.fiRun.models.ExerciseState
import me.stanis.apps.fiRun.models.HeartRate
import me.stanis.apps.fiRun.models.Speed
import me.stanis.apps.fiRun.models.enums.ExerciseStatus
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.models.enums.HeartRateAccuracy
import me.stanis.apps.fiRun.models.enums.HeartRateSource
import me.stanis.apps.fiRun.util.clock.Clock
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseWriterTest {
    private lateinit var db: Db
    private lateinit var exerciseDao: ExerciseDao
    private lateinit var distanceDao: DistanceDao
    private lateinit var heartRateDao: HeartRateDao
    private lateinit var speedDao: SpeedDao
    private lateinit var currentExerciseData: DataStore<CurrentExerciseData>
    private lateinit var underTest: ExerciseWriter

    private val clock = object : Clock {
        override fun now(): Instant = Instant.ofEpochSecond(0)
        override fun currentTimeMillis(): Long = 0
        override fun elapsedRealtime(): Long = 0
    }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, Db::class.java).allowMainThreadQueries().build()
        exerciseDao = db.exercise()
        distanceDao = db.distance()
        heartRateDao = db.heartRate()
        speedDao = db.speed()
        currentExerciseData = FakeDataStores.create(CurrentExerciseData.EMPTY)
        underTest =
            ExerciseWriter(
                exerciseDao,
                distanceDao,
                heartRateDao,
                speedDao,
                currentExerciseData,
                clock
            )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `create sets exercise ID and inserts ExerciseEntity`() = runTest() {
        assertNull(currentExerciseData.data.first().id)

        underTest.create(ExerciseType.IndoorRun)
        runCurrent()

        val id = currentExerciseData.data.first().id
        assertNotNull(id)
        assertEquals(
            ExerciseEntity(
                exerciseId = id,
                title = "",
                type = ExerciseType.IndoorRun,
                startTime = null,
                endTime = null,
                duration = null
            ),
            exerciseDao.get(id)
        )
    }

    @Test
    fun `writeStatusUpdates with InProgress writes CurrentExerciseData`() =
        runTest {
            val id = createExercise()
            val state = ExerciseState(
                status = ExerciseStatus.InProgress,
                lastUpdated = Duration.ofSeconds(1),
                startTime = Instant.ofEpochSecond(123),
                lastStateTransitionTime = Instant.ofEpochSecond(123),
                activeDuration = Duration.ofSeconds(321)
            )

            val stateUpdates = MutableSharedFlow<ExerciseState>()
            backgroundScope.launch { underTest.writeStateUpdates(stateUpdates) }
            runCurrent()
            stateUpdates.emit(state)
            runCurrent()

            assertEquals(
                CurrentExerciseData(
                    id = id,
                    inProgress = true,
                    lastStateTransitionTime = state.lastStateTransitionTime,
                    activeDuration = state.activeDuration
                ),
                currentExerciseData.data.first()
            )
        }

    @Test
    fun `writeStatusUpdates with InProgress updates ExerciseEntity`() =
        runTest {
            val id = createExercise()
            val state = ExerciseState(
                status = ExerciseStatus.InProgress,
                lastUpdated = Duration.ofSeconds(1),
                startTime = Instant.ofEpochSecond(123),
                lastStateTransitionTime = Instant.ofEpochSecond(123),
                activeDuration = Duration.ofSeconds(321)
            )

            val stateUpdates = MutableSharedFlow<ExerciseState>()
            backgroundScope.launch { underTest.writeStateUpdates(stateUpdates) }
            runCurrent()
            stateUpdates.emit(state)
            runCurrent()

            assertEquals(
                ExerciseEntity(
                    exerciseId = id,
                    title = "",
                    type = ExerciseType.IndoorRun,
                    startTime = ZonedDateTimeEntity.from(Instant.ofEpochSecond(123)),
                    endTime = null,
                    duration = state.getDuration(clock.now())
                ),
                exerciseDao.get(id)
            )
        }

    @Test
    fun `writeStatusUpdates with Ended updates ExerciseEntity`() = runTest {
        val id = createExercise()
        val state = ExerciseState(
            status = ExerciseStatus.Ended,
            lastUpdated = Duration.ofSeconds(60),
            startTime = Instant.ofEpochSecond(123),
            lastStateTransitionTime = Instant.ofEpochSecond(70),
            activeDuration = Duration.ofSeconds(456)
        )

        val stateUpdates = MutableSharedFlow<ExerciseState>()
        backgroundScope.launch { underTest.writeStateUpdates(stateUpdates) }
        runCurrent()
        stateUpdates.emit(state)
        runCurrent()

        assertEquals(
            ExerciseEntity(
                exerciseId = id,
                title = "",
                type = ExerciseType.IndoorRun,
                startTime = null,
                endTime = ZonedDateTimeEntity.from(state.lastStateTransitionTime),
                duration = state.getDuration(clock.now())
            ),
            exerciseDao.get(id)
        )
    }

    @Test
    fun `writeDistanceUpdates inserts DistanceEntity`() = runTest {
        val id = createExercise()
        val update = Distance(
            total = 5.0,
            instant = Instant.ofEpochSecond(432),
            exerciseDuration = Duration.ofSeconds(123)
        )

        val distanceUpdates = MutableSharedFlow<Distance>()
        backgroundScope.launch { underTest.writeDistanceUpdates(distanceUpdates) }
        runCurrent()
        distanceUpdates.emit(update)
        runCurrent()

        assertEquals(
            listOf(
                DistanceEntity(
                    id = 1,
                    exerciseId = id,
                    distance = update.total,
                    instant = update.instant,
                    exerciseDuration = update.exerciseDuration
                )
            ),
            exerciseDao.getWithData(id)?.distanceEntities
        )
    }

    @Test
    fun `writeSpeedUpdates inserts SpeedEntity`() = runTest {
        val id = createExercise()
        val update = Speed(
            current = 15.0,
            instant = Instant.ofEpochSecond(432),
            exerciseDuration = Duration.ofSeconds(123)
        )

        val distanceUpdates = MutableSharedFlow<Speed>()
        backgroundScope.launch { underTest.writeSpeedUpdates(distanceUpdates) }
        runCurrent()
        distanceUpdates.emit(update)
        runCurrent()

        assertEquals(
            listOf(
                SpeedEntity(
                    id = 1,
                    exerciseId = id,
                    speed = update.current,
                    instant = update.instant,
                    exerciseDuration = update.exerciseDuration
                )
            ),
            exerciseDao.getWithData(id)?.speedEntities
        )
    }

    @Test
    fun `writeHeartRateUpdates inserts HeartRateEntity`() = runTest {
        val id = createExercise()
        val update = HeartRate(
            bpm = 80,
            accuracy = HeartRateAccuracy.Medium,
            source = HeartRateSource.Internal,
            instant = Instant.ofEpochSecond(543),
            exerciseDuration = Duration.ofSeconds(644)
        )

        val hrUpdates = MutableSharedFlow<HeartRate>()
        backgroundScope.launch { underTest.writeHeartRateUpdates(hrUpdates) }
        runCurrent()
        hrUpdates.emit(update)
        runCurrent()

        assertEquals(
            listOf(
                HeartRateEntity(
                    id = 1,
                    exerciseId = id,
                    bpm = update.bpm,
                    accuracy = update.accuracy,
                    source = update.source,
                    instant = update.instant,
                    exerciseDuration = update.exerciseDuration
                )
            ),
            exerciseDao.getWithData(id)?.heartRateEntities
        )
    }

    private suspend fun createExercise(): UUID {
        val id = UUID.randomUUID()
        currentExerciseData.updateData {
            CurrentExerciseData(
                id = id,
                inProgress = false,
                lastStateTransitionTime = null,
                activeDuration = null
            )
        }
        exerciseDao.create(ExerciseEntityCreate(id, ExerciseType.IndoorRun))
        return id
    }
}
