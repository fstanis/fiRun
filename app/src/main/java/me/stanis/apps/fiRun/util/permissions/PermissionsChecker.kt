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

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import me.stanis.apps.fiRun.R

object PermissionsChecker {
    enum class PermissionCategory {
        BASIC,
        POLAR,
        INDOOR_RUN,
        OUTDOOR_RUN
    }

    val Set<PermissionCategory>.neededPermissions
        get() = mapNotNull { permissionsForCategory[it] }
            .fold(emptySet<String>()) { acc, permissions -> acc.union(permissions) }

    fun grantedCategories(context: Context) = PermissionCategory.values()
        .filter { it.checkAllGranted(context) }.toSet()

    fun grantedPermissions(context: Context) =
        allPermissions.filter { checkGranted(context, it) }.toSet()

    private fun PermissionCategory.checkAllGranted(context: Context) =
        (permissionsForCategory[this] ?: emptyList()).map { checkGranted(context, it) }.all { it }

    private fun checkGranted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(
            context,
            permission
        ) == PackageManager.PERMISSION_GRANTED

    val permissionTitles = mapOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION to R.string.permission_access_fine_location,
        android.Manifest.permission.BODY_SENSORS to R.string.permission_body_sensors,
        android.Manifest.permission.ACTIVITY_RECOGNITION to R.string.permission_activity_recognition,
        android.Manifest.permission.BLUETOOTH_CONNECT to R.string.permission_bluetooth_connect,
        android.Manifest.permission.BLUETOOTH_SCAN to R.string.permission_bluetooth_scan,
        android.Manifest.permission.BLUETOOTH to R.string.permission_bluetooth_connect,
        android.Manifest.permission.BLUETOOTH_ADMIN to R.string.permission_bluetooth_scan,
    )

    private val allPermissions
        get() =
            PermissionCategory.values().map { permissionsForCategory[it] }
                .fold(
                    emptySet<String>()
                ) { acc, permissions ->
                    acc.union(permissions ?: emptySet())
                }

    private val permissionsForCategory = mapOf(
        PermissionCategory.BASIC to emptyList(),
        PermissionCategory.POLAR to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            listOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN
            )
        },
        PermissionCategory.INDOOR_RUN to listOf(
            android.Manifest.permission.BODY_SENSORS,
            android.Manifest.permission.ACTIVITY_RECOGNITION
        ),
        PermissionCategory.OUTDOOR_RUN to listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.BODY_SENSORS,
            android.Manifest.permission.ACTIVITY_RECOGNITION
        )
    )
}
