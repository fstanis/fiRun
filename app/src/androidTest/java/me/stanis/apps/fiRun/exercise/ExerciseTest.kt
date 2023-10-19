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

@file:OptIn(ExperimentalTestApi::class)

package me.stanis.apps.fiRun.exercise

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.ExerciseState
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.ExerciseClientDelegate
import me.stanis.apps.fiRun.HealthServicesHelper
import me.stanis.apps.fiRun.MainActivity
import me.stanis.apps.fiRun.ServiceInfoHelper
import me.stanis.apps.fiRun.database.dao.ExerciseDao
import me.stanis.apps.fiRun.database.datastore.CurrentExerciseData
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.modules.HealthModule
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker.neededPermissions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(HealthModule::class)
class ExerciseTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule = GrantPermissionRule.grant(
        *setOf(PermissionsChecker.PermissionCategory.INDOOR_RUN).neededPermissions.toTypedArray()
    )

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val hsHelper = HealthServicesHelper(instrumentation)
    private val serviceInfoHelper = ServiceInfoHelper(instrumentation)

    @Inject
    lateinit var currentExerciseData: DataStore<CurrentExerciseData>

    @Inject
    lateinit var exerciseDao: ExerciseDao

    private val exerciseClientDelegate = ExerciseClientDelegate.from(instrumentation.targetContext)

    @BindValue
    val exerciseClientBinding: ExerciseClient = exerciseClientDelegate

    @Before
    fun setUp() {
        hiltRule.inject()
        hsHelper.enableSynthetic()
        Thread.sleep(1_000)
    }

    @After
    fun tearDown() {
        hsHelper.disableSynthetic()
    }

    @Test
    fun startingExerciseSetsExerciseId() = runTest {
        val currentExercise = currentExerciseData.data.stateIn(backgroundScope)
        composeTestRule.waitUntilAtLeastOneExists(hasText("Indoor run"))

        assertNull(currentExercise.value.id)
        composeTestRule.onNodeWithText("Indoor run").performClick()
        exerciseClientDelegate.awaitExerciseState(ExerciseState.ACTIVE)
        composeTestRule.awaitIdle()

        assertNotNull(currentExercise.value.id)
    }

    @Test
    fun startingExercisePutsServiceInForeground() = runTest {
        composeTestRule.waitUntilDoesNotExist(hasTestTag("loading"), timeoutMillis = 10_000)

        assertFalse(serviceInfoHelper.exerciseServiceInfo.foreground)
        composeTestRule.onNodeWithText("Indoor run").performClick()
        exerciseClientDelegate.awaitExerciseState(ExerciseState.ACTIVE)
        composeTestRule.awaitIdle()

        assertTrue(serviceInfoHelper.exerciseServiceInfo.foreground)
    }

    @LargeTest
    @Test
    fun exerciseAddsDataToRoom() = runTest(timeout = 120.seconds) {
        hsHelper.startExercise(ExerciseType.IndoorRun)
        composeTestRule.waitUntilAtLeastOneExists(hasText("Indoor run"))
        composeTestRule.onNodeWithText("Indoor run").performClick()
        exerciseClientDelegate.clearDataPoints()
        exerciseClientDelegate.awaitExerciseState(ExerciseState.ACTIVE)
        runCurrent()
        composeTestRule.awaitIdle()

        val currentExerciseId = currentExerciseData.data.mapNotNull { it.id }.first()
        var data = exerciseDao.getWithData(currentExerciseId)
        assertNotNull(data)
        assertNotNull(data.exerciseEntity.startTime)
        assertNull(data.exerciseEntity.endTime)
        assertEquals(ExerciseType.IndoorRun, data.exerciseEntity.type)

        exerciseClientDelegate.awaitExerciseDuration(90.seconds)
        composeTestRule.onNodeWithContentDescription("Pause exercise").performClick()
        composeTestRule.onNodeWithContentDescription("End exercise").performClick()
        exerciseClientDelegate.awaitExerciseState(ExerciseState.ENDED)
        runCurrent()
        composeTestRule.awaitIdle()

        val dataPointContainer = exerciseClientDelegate.dataPointContainer.first()
        val hrCount = dataPointContainer.getData(DataType.HEART_RATE_BPM).count()
        val speedCount = dataPointContainer.getData(DataType.SPEED).count()
        data = exerciseDao.getWithData(currentExerciseId)
        assertNotNull(data)
        assertNotNull(data.exerciseEntity.startTime)
        assertNotNull(data.exerciseEntity.endTime)
        assertTrue(data.distanceEntities.isNotEmpty())
        assertEquals(hrCount, data.heartRateEntities.count())
        assertEquals(speedCount, data.speedEntities.count())
        hsHelper.stopExercise()
    }
}
