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

package me.stanis.apps.fiRun.ui.home

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.BluetoothConnected
import androidx.compose.material.icons.outlined.BluetoothSearching
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Nature
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import me.stanis.apps.fiRun.R
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.services.polar.ConnectionState
import me.stanis.apps.fiRun.ui.Screen
import me.stanis.apps.fiRun.ui.exercise.ExerciseScreen
import me.stanis.apps.fiRun.ui.home.model.UiState
import me.stanis.apps.fiRun.ui.navigate
import me.stanis.apps.fiRun.ui.settings.SettingsScreen
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker.PermissionCategory
import me.stanis.apps.fiRun.util.permissions.PermissionsManager

@Module
@InstallIn(SingletonComponent::class)
object HomeScreen : Screen {
    @SuppressLint("ComposableNaming")
    @Composable
    override fun content(navController: NavHostController, navEntry: NavBackStackEntry) {
        val viewModel = hiltViewModel<HomeViewModel>()
        val uiState by viewModel.uiStateFlow.collectAsState()
        HomePage(
            status = uiState.homeStatus,
            onStartExercise = {
                viewModel.startExercise(it)
                navController.navigate(ExerciseScreen)
            },
            onOpenExercise = {
                navController.navigate(ExerciseScreen)
            },
            onSettingsClick = {
                navController.navigate(SettingsScreen)
            },
            onToggleHrDevice = {
                if (it) {
                    viewModel.connectPolar()
                } else {
                    viewModel.disconnectPolar()
                }
            },
            connectionStatus = uiState.connectionStatus,
            canConnectPolar = uiState.canConnectPolar
        )
    }

    @Composable
    private fun HomePage(
        status: UiState.HomeStatus,
        onStartExercise: (ExerciseType) -> Unit,
        onOpenExercise: () -> Unit,
        onSettingsClick: () -> Unit,
        onToggleHrDevice: (Boolean) -> Unit,
        connectionStatus: ConnectionState.ConnectionStatus,
        canConnectPolar: Boolean
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberScalingLazyListState(initialCenterItemIndex = 0),
            autoCentering = AutoCenteringParams(itemIndex = 0),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp)
        ) {
            when (status) {
                UiState.HomeStatus.Default -> exerciseChips(
                    onStartExercise = onStartExercise
                )

                UiState.HomeStatus.ExerciseInProgress -> item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ChipDefaults.primaryChipColors(),
                        onClick = onOpenExercise,
                        label = {
                            Text("Exercise in progress")
                        },
                        icon = {
                            Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                        }
                    )
                }

                UiState.HomeStatus.Loading -> item {
                    CircularProgressIndicator(modifier = Modifier.testTag("loading"))
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        colors = ButtonDefaults.secondaryButtonColors(),
                        onClick = onSettingsClick
                    ) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = "Settings")
                    }

                    ToggleButton(
                        checked =
                        connectionStatus == ConnectionState.ConnectionStatus.READY,
                        enabled = canConnectPolar,
                        onCheckedChange = onToggleHrDevice
                    ) {
                        Icon(
                            imageVector = when (connectionStatus) {
                                ConnectionState.ConnectionStatus.READY ->
                                    Icons.Outlined.BluetoothConnected

                                ConnectionState.ConnectionStatus.PREPARING ->
                                    Icons.Outlined.BluetoothSearching

                                else ->
                                    Icons.Outlined.Bluetooth
                            },
                            contentDescription = null
                        )
                    }
                }
            }
        }
    }

    private fun ScalingLazyListScope.exerciseChips(
        onStartExercise: (ExerciseType) -> Unit
    ) {
        item {
            val permissionsManager = PermissionsManager.Local.current
            val hasPermissions by permissionsManager.isGranted(PermissionCategory.INDOOR_RUN)
            ExerciseChip(
                text = "Indoor run",
                icon = Icons.Outlined.FitnessCenter,
                hasPermissions = hasPermissions,
                onRequestPermissions = {
                    permissionsManager.requestCategory(PermissionCategory.INDOOR_RUN)
                },
                onStart = { onStartExercise(ExerciseType.IndoorRun) }
            )
        }

        item {
            val permissionsManager = PermissionsManager.Local.current
            val hasPermissions by permissionsManager.isGranted(PermissionCategory.OUTDOOR_RUN)
            ExerciseChip(
                text = "Outdoor run",
                icon = Icons.Outlined.Nature,
                hasPermissions = hasPermissions,
                onRequestPermissions = {
                    permissionsManager.requestCategory(PermissionCategory.OUTDOOR_RUN)
                },
                onStart = { onStartExercise(ExerciseType.OutdoorRun) }
            )
        }
    }

    @Composable
    private fun ExerciseChip(
        text: String,
        icon: ImageVector,
        hasPermissions: Boolean,
        onRequestPermissions: () -> Unit,
        onStart: () -> Unit
    ) {
        val secondaryLabel: @Composable RowScope.() -> Unit = @Composable {
            Text(stringResource(R.string.missing_permissions))
        }
        Chip(
            modifier = Modifier.fillMaxWidth(),
            colors = ChipDefaults.primaryChipColors(),
            onClick = if (hasPermissions) onStart else onRequestPermissions,
            label = {
                Text(text)
            },
            secondaryLabel = secondaryLabel.takeUnless { hasPermissions },
            icon = {
                Icon(imageVector = icon, contentDescription = null)
            }
        )
    }

    @Provides
    @IntoSet
    override fun provide(): Screen = this
}
