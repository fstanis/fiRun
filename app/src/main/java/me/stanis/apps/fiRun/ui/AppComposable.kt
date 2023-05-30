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

package me.stanis.apps.fiRun.ui

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import me.stanis.apps.fiRun.util.permissions.DefaultPermissionsManager
import me.stanis.apps.fiRun.util.permissions.PermissionsDialog
import me.stanis.apps.fiRun.util.permissions.PermissionsManager

@Composable
fun AppComposable(
    permissionsManager: DefaultPermissionsManager,
    screens: Set<Screen>,
    navController: NavHostController,
    startDestination: Screen
) {
    CompositionLocalProvider(PermissionsManager.Local provides permissionsManager) {
        SwipeDismissableNavHost(
            navController = navController, startDestination = startDestination.route
        ) {
            for (screen in screens) {
                composable(
                    route = screen.route,
                    content = { screen.content(navController, it) })
            }
        }
    }
    PermissionsDialog(permissionsManager)
}

interface Screen {
    val route: String
        get() = this::class.java.name

    @SuppressLint("ComposableNaming")
    @Composable
    fun content(navController: NavHostController, navEntry: NavBackStackEntry)

    fun provide(): Screen
}

inline fun NavController.navigate(screen: Screen) = navigate(screen.route)
inline fun NavController.navigate(screen: Screen, noinline builder: NavOptionsBuilder.() -> Unit) =
    navigate(screen.route, builder)
