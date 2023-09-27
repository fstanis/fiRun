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

package me.stanis.apps.fiRun.util.permissions

import android.Manifest
import android.app.Application
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker.neededPermissions
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DefaultPermissionManagerTest {
    private val app: Application = getApplicationContext()
    private val shadowApp = shadowOf(app)

    @Test
    fun `openAppSettings opens settings`() {
        val underTest = DefaultPermissionsManager(app)

        underTest.openAppSettings()
        val intent = shadowApp.nextStartedActivity

        assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, intent.action)
    }

    @Test
    fun `permissionRequests launches permission requests`() = runTest {
        val underTest = DefaultPermissionsManager(app)

        val launcher: ActivityResultLauncher<Array<String>> = mock()
        underTest.permissionRequests(launcher).launchIn(backgroundScope)

        underTest.requestPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        runCurrent()

        verify(launcher).launch(eq(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)))
    }

    @Test
    fun `permissionRequests doesn't launch for granted permissions`() = runTest {
        shadowApp.grantPermissions(Manifest.permission.BODY_SENSORS)
        val underTest = DefaultPermissionsManager(app)
        val launcher: ActivityResultLauncher<Array<String>> = mock()
        underTest.permissionRequests(launcher).launchIn(backgroundScope)

        underTest.requestPermission(Manifest.permission.BODY_SENSORS)
        runCurrent()

        verify(launcher, never()).launch(any())
    }

    @Test
    fun `requestCategory sends requests for every valid category`() = runTest {
        for (category in PermissionsChecker.PermissionCategory.values()) {
            `requestCategory sends requests for category`(category)
        }
    }

    private suspend fun TestScope.`requestCategory sends requests for category`(
        category: PermissionsChecker.PermissionCategory
    ) {
        val underTest = DefaultPermissionsManager(app)
        val launcher: ActivityResultLauncher<Array<String>> = mock()
        underTest.permissionRequests(launcher).launchIn(backgroundScope)

        underTest.requestCategory(category)
        underTest.requestAllNeededPermissions()
        runCurrent()

        val permissions = setOf(category).neededPermissions
        if (permissions.isEmpty()) {
            verify(launcher, never()).launch(any())
        } else {
            verify(launcher).launch(eq(permissions.toTypedArray()))
        }
    }
}
