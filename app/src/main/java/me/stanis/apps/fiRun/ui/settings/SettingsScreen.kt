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

package me.stanis.apps.fiRun.ui.settings

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.wear.compose.foundation.lazy.AutoCenteringParams
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Checkbox
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import me.stanis.apps.fiRun.ui.Screen
import me.stanis.apps.fiRun.ui.devices.DevicesScreen
import me.stanis.apps.fiRun.ui.navigate

@Module
@InstallIn(SingletonComponent::class)
object SettingsScreen : Screen {
    @SuppressLint("ComposableNaming")
    @Composable
    override fun content(navController: NavHostController, navEntry: NavBackStackEntry) {
        val viewModel = hiltViewModel<SettingsViewModel>()
        val onlyHighHrAccuracy by viewModel.onlyHighHrAccuracyState.collectAsState()
        val keepScreenOn by viewModel.keepScreenOn.collectAsState()
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = rememberScalingLazyListState(initialCenterItemIndex = 0),
            autoCentering = AutoCenteringParams(itemIndex = 0)
        ) {
            item {
                Chip(
                    modifier = Modifier.fillMaxWidth(),
                    colors = ChipDefaults.secondaryChipColors(),
                    onClick = { navController.navigate(DevicesScreen) },
                    label = {
                        Text("HR provider")
                    }
                )
            }
            item {
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    onCheckedChange = { viewModel.setHeartRateAccuracy(it) },
                    checked = onlyHighHrAccuracy,
                    label = {
                        Text("Only high HR accuracy")
                    },
                    toggleControl = {
                        Checkbox(checked = onlyHighHrAccuracy)
                    }
                )
            }
            item {
                ToggleChip(
                    modifier = Modifier.fillMaxWidth(),
                    onCheckedChange = { viewModel.setKeepScreenOn(it) },
                    checked = keepScreenOn,
                    label = {
                        Text("Keep screen on during exercise")
                    },
                    toggleControl = {
                        Checkbox(checked = keepScreenOn)
                    }
                )
            }
        }
    }

    @Provides
    @IntoSet
    override fun provide(): Screen = this
}
