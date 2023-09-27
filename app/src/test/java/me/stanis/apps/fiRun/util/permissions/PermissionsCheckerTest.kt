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

package me.stanis.apps.fiRun.util.permissions

import android.Manifest
import android.app.Application
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kotlin.test.assertEquals
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker.PermissionCategory
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker.neededPermissions
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@RunWith(RobolectricTestRunner::class)
class PermissionsCheckerTest {
    private val app: Application = getApplicationContext()
    private fun grantPermissions(permissionNames: Collection<String>) =
        Shadows.shadowOf(app).grantPermissions(*permissionNames.toTypedArray())

    @Test
    fun `grantedPermissions returns all granted permissions`() {
        val allPermissions = setOf(
            PermissionCategory.BASIC,
            PermissionCategory.POLAR,
            PermissionCategory.INDOOR_RUN,
            PermissionCategory.OUTDOOR_RUN
        ).neededPermissions
        assertEquals(emptySet(), PermissionsChecker.grantedPermissions(app))

        grantPermissions(allPermissions)

        assertEquals(allPermissions, PermissionsChecker.grantedPermissions(app))
    }

    @Test
    fun `grantedPermissions ignores other granted permissions`() {
        grantPermissions(setOf(Manifest.permission.ACCESS_WIFI_STATE))
        assertEquals(emptySet(), PermissionsChecker.grantedPermissions(app))
    }

    @Test
    fun `grantedCategories only shows categories that are complete`() {
        assertEquals(setOf(PermissionCategory.BASIC), PermissionsChecker.grantedCategories(app))

        grantPermissions(setOf(PermissionCategory.INDOOR_RUN).neededPermissions)

        assertEquals(
            setOf(PermissionCategory.BASIC, PermissionCategory.INDOOR_RUN),
            PermissionsChecker.grantedCategories(app)
        )
    }
}
