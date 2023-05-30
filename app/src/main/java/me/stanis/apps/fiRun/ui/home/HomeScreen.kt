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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleButton
import androidx.wear.compose.material.dialog.Dialog
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import me.stanis.apps.fiRun.R
import me.stanis.apps.fiRun.services.exercise.ExerciseStatus
import me.stanis.apps.fiRun.services.polar.DeviceConnectionStatus
import me.stanis.apps.fiRun.ui.Screen
import me.stanis.apps.fiRun.ui.navigate
import me.stanis.apps.fiRun.ui.settings.SettingsScreen
import me.stanis.apps.fiRun.util.heartrate.HeartRate
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker.PermissionCategory
import me.stanis.apps.fiRun.util.permissions.PermissionsManager
import java.time.Duration
import kotlin.time.toKotlinDuration

@Module
@InstallIn(SingletonComponent::class)
object HomeScreen : Screen {
    @SuppressLint("ComposableNaming")
    @Composable
    override fun content(navController: NavHostController, navEntry: NavBackStackEntry) {
        val viewModel = hiltViewModel<HomeViewModel>()
        val status by viewModel.status.collectAsState()
        val polarStatus by viewModel.deviceConnectionStatus.collectAsState()
        val canConnectPolar by viewModel.canConnectPolar.collectAsState()
        val polarHrData by viewModel.polarHrData.collectAsState()
        val exerciseHrData by viewModel.exerciseHrData.collectAsState()
        when (status.state) {
            ExerciseStatus.ExerciseState.InProgress -> ExercisePage(
                modifier = Modifier.fillMaxSize(),
                heartRate = polarHrData ?: exerciseHrData,
                distance = status.exerciseInfo.totalDistance,
                duration = status.exerciseInfo.duration,
                pace = status.exerciseInfo.derivedPace,
                onEndExerciseClick = viewModel::endExercise
            )

            ExerciseStatus.ExerciseState.NotStarted -> HomePage(
                onStartIndoorClick = viewModel::startExercise,
                onStartOutdoorClick = viewModel::startExercise,
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
                deviceConnectionStatus = polarStatus,
                canConnectPolar = canConnectPolar
            )

            ExerciseStatus.ExerciseState.Loading, ExerciseStatus.ExerciseState.Ended ->
                LoadingPage(Modifier.fillMaxSize())

            else -> {}
        }
        Dialog(
            showDialog = status.state == ExerciseStatus.ExerciseState.Ended,
            onDismissRequest = viewModel::resetExercise,
        ) {
            SummaryPage(
                heartRate = polarHrData ?: exerciseHrData,
                distance = status.exerciseInfo.totalDistance,
                duration = status.exerciseInfo.duration,
                pace = status.exerciseInfo.derivedPace
            )
        }
    }

    @Composable
    private fun LoadingPage(modifier: Modifier) {
        Box(
            modifier,
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }

    @Composable
    private fun HomePage(
        onStartIndoorClick: () -> Unit,
        onStartOutdoorClick: () -> Unit,
        onSettingsClick: () -> Unit,
        onToggleHrDevice: (Boolean) -> Unit,
        deviceConnectionStatus: DeviceConnectionStatus,
        canConnectPolar: Boolean
    ) {
        val permissionsManager = PermissionsManager.Local.current
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberScalingLazyListState(initialCenterItemIndex = 0),
            autoCentering = AutoCenteringParams(itemIndex = 0),
            verticalArrangement = Arrangement.spacedBy(space = 8.dp),
        ) {
            item {
                val hasPermissions by permissionsManager.isGranted(PermissionCategory.INDOOR_RUN)
                ExerciseChip(
                    text = "Indoor run",
                    icon = Icons.Outlined.FitnessCenter,
                    hasPermissions = hasPermissions,
                    onRequestPermissions = {
                        permissionsManager.requestCategory(PermissionCategory.INDOOR_RUN)
                    },
                    onStart = onStartIndoorClick
                )
            }

            item {
                val hasPermissions by permissionsManager.isGranted(PermissionCategory.OUTDOOR_RUN)
                ExerciseChip(
                    text = "Outdoor run",
                    icon = Icons.Outlined.Nature,
                    hasPermissions = hasPermissions,
                    onRequestPermissions = {
                        permissionsManager.requestCategory(PermissionCategory.OUTDOOR_RUN)
                    },
                    onStart = onStartOutdoorClick
                )
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        colors = ButtonDefaults.secondaryButtonColors(),
                        onClick = onSettingsClick
                    ) {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                    }

                    ToggleButton(
                        checked = deviceConnectionStatus is DeviceConnectionStatus.Connected,
                        enabled = canConnectPolar,
                        onCheckedChange = onToggleHrDevice
                    ) {
                        Icon(
                            imageVector = when (deviceConnectionStatus) {
                                is DeviceConnectionStatus.Connected -> Icons.Outlined.BluetoothConnected
                                is DeviceConnectionStatus.Connecting -> Icons.Outlined.BluetoothSearching
                                else -> Icons.Outlined.Bluetooth
                            },
                            contentDescription = null
                        )
                    }
                }
            }
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

    @Composable
    private fun ExercisePage(
        modifier: Modifier,
        heartRate: HeartRate?,
        distance: Double,
        pace: Duration,
        duration: Duration,
        onEndExerciseClick: () -> Unit
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Heart rate: ${heartRate?.heartRate ?: "?"}")
            Text(
                "Pace: ${
                    pace.toKotlinDuration().toComponents { minutes, seconds, _ ->
                        String.format(
                            "%d:%02d",
                            minutes,
                            seconds
                        )
                    }
                }"
            )
            Text("Distance: ${String.format("%.2f", distance / 1000)}km")
            Text("Duration: ${duration.seconds}s")
            Button(onClick = onEndExerciseClick) {
                Icon(imageVector = Icons.Outlined.Stop, contentDescription = null)
            }
        }
    }

    @Composable
    private fun SummaryPage(
        heartRate: HeartRate?,
        distance: Double,
        pace: Duration,
        duration: Duration
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Heart rate: ${heartRate?.heartRate ?: "?"}")
            Text(
                "Pace: ${
                    pace.toKotlinDuration().toComponents { minutes, seconds, _ ->
                        String.format(
                            "%d:%02d",
                            minutes,
                            seconds
                        )
                    }
                }"
            )
            Text("Distance: ${String.format("%.2f", distance / 1000)}km")
            Text("Duration: ${duration.seconds}s")
        }
    }

    @Provides
    @IntoSet
    override fun provide(): Screen = this
}
