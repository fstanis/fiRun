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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

interface PermissionsManager {
    fun requestCategory(category: PermissionsChecker.PermissionCategory)

    @Composable
    fun isGranted(category: PermissionsChecker.PermissionCategory): State<Boolean>

    companion object {
        val Local = staticCompositionLocalOf<PermissionsManager> {
            object : PermissionsManager {
                override fun requestCategory(category: PermissionsChecker.PermissionCategory) {}

                @Composable
                override fun isGranted(category: PermissionsChecker.PermissionCategory) =
                    remember { mutableStateOf(false) }
            }
        }
    }
}
