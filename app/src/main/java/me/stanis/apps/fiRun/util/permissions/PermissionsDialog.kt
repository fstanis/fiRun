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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.dialog.Dialog
import kotlinx.coroutines.launch
import me.stanis.apps.fiRun.R

@Composable
fun PermissionsDialog(
    permissionsManager: DefaultPermissionsManager,
    requestImmediately: Boolean = false
) {
    val allPermissionsGranted by permissionsManager.allNeededPermissionsGranted.collectAsState(
        initial = true
    )
    val neededPermissions by permissionsManager.neededPermissions.collectAsState(initial = emptySet())
    val grantedPermissions by permissionsManager.grantedPermissions.collectAsState(initial = emptySet())
    val scope = rememberCoroutineScope()
    if (requestImmediately) {
        LaunchedEffect(key1 = neededPermissions) {
            permissionsManager.requestAllNeededPermissions()
        }
    }
    PermissionsDialog(
        allNeededPermissionsGranted = allPermissionsGranted,
        neededPermissions = neededPermissions,
        grantedPermissions = grantedPermissions,
        onRequestPermission = { scope.launch { permissionsManager.requestPermission(it) } },
        onOpenSettings = { permissionsManager.openAppSettings() },
        onCancel = { permissionsManager.cancelCategoryRequests() }
    )
}

@Composable
private fun PermissionsDialog(
    allNeededPermissionsGranted: Boolean,
    neededPermissions: Set<String>,
    grantedPermissions: Set<String>,
    onRequestPermission: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(
        showDialog = !allNeededPermissionsGranted,
        onDismissRequest = onCancel,
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberScalingLazyListState(initialCenterItemIndex = 0),
            autoCentering = AutoCenteringParams(itemIndex = 0),
        ) {
            item {
                Text(stringResource(R.string.permission_required))
            }
            for (permission in neededPermissions) {
                item {
                    val granted by derivedStateOf {
                        grantedPermissions.contains(permission)
                    }
                    PermissionChip(
                        title = stringResource(PermissionsChecker.permissionTitles[permission]!!),
                        granted = granted,
                    ) { onRequestPermission(permission) }
                }
            }
            item {
                Spacer(modifier = Modifier.size(8.dp))
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.secondaryChipColors(),
                    label = {
                        Text(stringResource(R.string.open_app_settings))
                    },
                    icon = {
                        Icon(imageVector = Icons.Outlined.Settings, contentDescription = null)
                    },
                    onClick = onOpenSettings
                )
            }
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text(stringResource(R.string.cancel_chip))
                    },
                    onClick = onCancel
                )
            }
        }
    }
}

@Composable
private fun PermissionChip(
    title: String,
    granted: Boolean,
    onRequestPermission: () -> Unit
) {
    ToggleChip(
        modifier = Modifier.fillMaxWidth(),
        checked = granted,
        enabled = !granted,
        onCheckedChange = { onRequestPermission() },
        label = { Text(title) },
        toggleControl = { Checkbox(checked = granted) }
    )
}
