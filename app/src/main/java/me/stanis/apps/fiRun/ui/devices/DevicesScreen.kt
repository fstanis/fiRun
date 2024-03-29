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

package me.stanis.apps.fiRun.ui.devices

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.RadioButton
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import me.stanis.apps.fiRun.R
import me.stanis.apps.fiRun.models.HrDeviceInfo
import me.stanis.apps.fiRun.services.polar.SearchState
import me.stanis.apps.fiRun.ui.Screen
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker
import me.stanis.apps.fiRun.util.permissions.PermissionsManager

@Module
@InstallIn(SingletonComponent::class)
object DevicesScreen : Screen {
    @SuppressLint("ComposableNaming")
    @Composable
    override fun content(navController: NavHostController, navEntry: NavBackStackEntry) {
        val viewModel = hiltViewModel<DevicesViewModel>()
        val uiState by viewModel.uiStateFlow.collectAsState()
        val permissionsManager = PermissionsManager.Local.current
        if (uiState.searchState.status == SearchState.SearchStatus.NoSearch) {
            MainPage(
                knownDevices = uiState.knownDevices,
                disconnectDevice = viewModel::disconnectDevice,
                connectDevice = viewModel::connectDevice,
                startSearch = viewModel::startSearch,
                permissionsManager = permissionsManager
            )
        } else {
            SearchPage(
                devices = uiState.searchState.devicesFound,
                onSelect = {
                    viewModel.saveDevice(it)
                    viewModel.stopSearch()
                },
                onCancel = {
                    viewModel.stopSearch()
                }
            )
        }
    }

    @Composable
    fun MainPage(
        knownDevices: Set<HrDeviceInfo>,
        disconnectDevice: (String) -> Unit,
        connectDevice: (String) -> Unit,
        startSearch: () -> Unit,
        permissionsManager: PermissionsManager
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberScalingLazyListState(initialCenterItemIndex = 0),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item {
                WatchHrChip()
            }
            for (device in knownDevices) {
                item {
                    DeviceChip(
                        device = device,
                        onConnect = { connectDevice(device.deviceId) },
                        onDisconnect = { disconnectDevice(device.deviceId) }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.size(8.dp))
            }
            item {
                val hasPermissions by permissionsManager.isGranted(
                    category = PermissionsChecker.PermissionCategory.POLAR
                )
                ConnectChip(
                    hasPermissions = hasPermissions,
                    onRequestPermissions = {
                        permissionsManager.requestCategory(
                            PermissionsChecker.PermissionCategory.POLAR
                        )
                    },
                    onClick = {
                        startSearch()
                    }
                )
            }
        }
    }

    @Composable
    fun SearchPage(
        devices: Set<HrDeviceInfo>,
        onSelect: (HrDeviceInfo) -> Unit,
        onCancel: () -> Unit
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberScalingLazyListState(initialCenterItemIndex = 0),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            if (devices.isEmpty()) {
                item {
                    CircularProgressIndicator()
                }
            }
            for (device in devices) {
                item {
                    Chip(
                        modifier = Modifier.fillMaxWidth(),
                        colors = ChipDefaults.primaryChipColors(),
                        onClick = { onSelect(device) },
                        label = { Text(device.name) },
                        secondaryLabel = { Text(device.deviceId) },
                        icon = {
                            Icon(
                                imageVector = Icons.Outlined.MonitorHeart,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.size(8.dp))
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.primaryChipColors(),
                    onClick = onCancel,
                    label = { Text("Cancel search") },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Cancel,
                            contentDescription = null
                        )
                    }
                )
            }
        }
    }

    @Composable
    private fun WatchHrChip() {
        ToggleChip(
            modifier = Modifier.fillMaxWidth(),
            checked = true,
            enabled = false,
            onCheckedChange = { },
            label = { Text("Built-in sensor") },
            toggleControl = { Checkbox(checked = true) }
        )
    }

    @Composable
    private fun NoDeviceChip() {
        ToggleChip(
            modifier = Modifier.fillMaxWidth(),
            checked = false,
            onCheckedChange = { },
            label = { Text(stringResource(R.string.device_none)) },
            secondaryLabel = { Text(stringResource(R.string.pair_to_add)) },
            enabled = false,
            toggleControl = { RadioButton(selected = false) }
        )
    }

    @Composable
    private fun DeviceChip(
        device: HrDeviceInfo,
        onConnect: () -> Unit,
        onDisconnect: () -> Unit
    ) {
        ToggleChip(
            modifier = Modifier.fillMaxWidth(),
            checked = device.canStreamHr,
            onCheckedChange = { if (it) onConnect() else onDisconnect() },
            label = { Text(device.name) },
            secondaryLabel = { Text(device.deviceId) },
            toggleControl = {
                if (device.isPreparing) {
                    CircularProgressIndicator()
                } else {
                    Checkbox(checked = device.canStreamHr)
                }
            }
        )
    }

    @Composable
    private fun ConnectChip(
        hasPermissions: Boolean,
        onRequestPermissions: () -> Unit,
        onClick: () -> Unit
    ) {
        val secondaryLabel: @Composable RowScope.() -> Unit = @Composable {
            Text(stringResource(R.string.missing_permissions))
        }
        Chip(
            modifier = Modifier.fillMaxWidth(),
            colors = ChipDefaults.primaryChipColors(),
            onClick = if (hasPermissions) onClick else onRequestPermissions,
            label = {
                Text(stringResource(R.string.pair_new))
            },
            secondaryLabel = secondaryLabel.takeUnless { hasPermissions },
            icon = {
                Icon(imageVector = Icons.Outlined.Search, contentDescription = null)
            }
        )
    }

    @Provides
    @IntoSet
    override fun provide(): Screen = this
}
