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

package me.stanis.apps.fiRun.settings

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.MainActivity
import me.stanis.apps.fiRun.database.datastore.SettingsData
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class SettingsScreenTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var settingsData: DataStore<SettingsData>

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun changingSettingsUpdatesDataStore() = runTest(timeout = 30.seconds) {
        val settingsState = settingsData.data.stateIn(backgroundScope)
        composeTestRule.waitUntilAtLeastOneExists(hasContentDescription("Settings"))
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.awaitIdle()
        assertFalse(settingsState.value.onlyHighHrAccuracy)
        assertFalse(settingsState.value.keepScreenOn)

        composeTestRule.onNodeWithText("Only high HR accuracy").performClick()
        composeTestRule.awaitIdle()
        assertTrue(settingsState.value.onlyHighHrAccuracy)

        composeTestRule.onNodeWithText("Keep screen on during exercise").performClick()
        composeTestRule.awaitIdle()
        assertTrue(settingsState.value.keepScreenOn)
    }
}
