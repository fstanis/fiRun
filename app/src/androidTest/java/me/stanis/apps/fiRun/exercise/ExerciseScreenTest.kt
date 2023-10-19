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
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.health.services.client.ExerciseClient
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.ExerciseClientDelegate
import me.stanis.apps.fiRun.HealthServicesHelper
import me.stanis.apps.fiRun.MainActivity
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.modules.HealthModule
import me.stanis.apps.fiRun.services.exercise.heartRateSamples
import me.stanis.apps.fiRun.services.exercise.totalDistance
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker.neededPermissions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
@UninstallModules(HealthModule::class)
class ExerciseScreenTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val permissionRule = GrantPermissionRule.grant(
        *setOf(PermissionsChecker.PermissionCategory.INDOOR_RUN).neededPermissions.toTypedArray()
    )

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val hsTestUtil = HealthServicesHelper(instrumentation)

    private val exerciseClientDelegate = ExerciseClientDelegate.from(instrumentation.targetContext)

    @BindValue
    val exerciseClientBinding: ExerciseClient = exerciseClientDelegate

    @Before
    fun setUp() {
        hiltRule.inject()
        hsTestUtil.enableSynthetic()
        Thread.sleep(1_000)
    }

    @After
    fun tearDown() {
        hsTestUtil.disableSynthetic()
    }

    @Test
    fun uiShowsLatestHeartRateAndDistance() = runTest(timeout = 30.seconds) {
        hsTestUtil.startExercise(ExerciseType.IndoorRun)
        composeTestRule.waitUntilAtLeastOneExists(hasText("Indoor run"))
        composeTestRule.onNodeWithText("Indoor run").performClick()
        composeTestRule.waitUntilAtLeastOneExists(hasText("00:00", true))

        val collector = launch {
            exerciseClientDelegate.updates.conflate().collectLatest {
                composeTestRule.awaitIdle()
                val heartRate = it.heartRateSamples.lastOrNull()?.value?.roundToInt()
                if (heartRate != null) {
                    composeTestRule.onNodeWithTag("heartRate")
                        .assertTextContains(heartRate.toString(), substring = true)
                }
                val distance = it.totalDistance?.total?.roundToInt()
                if (distance != null) {
                    composeTestRule.onNodeWithTag("distance")
                        .assertTextContains(distance.toString(), substring = true)
                }
            }
        }

        exerciseClientDelegate.awaitExerciseDuration(10.seconds)
        collector.cancel()
        hsTestUtil.stopExercise()
    }
}
