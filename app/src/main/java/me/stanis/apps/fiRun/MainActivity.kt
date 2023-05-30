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

package me.stanis.apps.fiRun

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import dagger.hilt.android.AndroidEntryPoint
import me.stanis.apps.fiRun.ui.AppComposable
import me.stanis.apps.fiRun.ui.Screen
import me.stanis.apps.fiRun.ui.home.HomeScreen
import me.stanis.apps.fiRun.util.permissions.DefaultPermissionsManager
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var screens: Set<@JvmSuppressWildcards Screen>

    @Inject
    lateinit var permissionsManager: DefaultPermissionsManager

    private val permissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            permissionsManager.recheckPermissions()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionsManager.launchPermissionRequests(this, permissionRequest)
        setContent {
            val navController = rememberSwipeDismissableNavController()
            AppComposable(
                permissionsManager,
                screens,
                navController,
                startDestination = HomeScreen
            )
        }
    }
}
