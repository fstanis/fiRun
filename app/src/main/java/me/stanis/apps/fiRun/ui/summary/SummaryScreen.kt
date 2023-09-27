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

package me.stanis.apps.fiRun.ui.summary

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Text
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import java.time.Duration
import me.stanis.apps.fiRun.ui.Screen

@Module
@InstallIn(SingletonComponent::class)
object SummaryScreen : Screen {
    @SuppressLint("ComposableNaming")
    @Composable
    override fun content(navController: NavHostController, navEntry: NavBackStackEntry) {
        val viewModel = hiltViewModel<SummaryViewModel>()
        val exerciseData by viewModel.exerciseData.collectAsState()
        exerciseData?.let { lastExercise ->
            SummaryPage(
                distance = lastExercise.distanceEntities.lastOrNull()?.distance ?: 0.0,
                duration = lastExercise.exerciseEntity.duration ?: Duration.ZERO
            )
        }
    }

    @Composable
    private fun SummaryPage(
        distance: Double,
        duration: Duration
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("Distance: ${String.format("%.2f", distance / 1000)}km")
            Text("Duration: ${duration.seconds}s")
        }
    }

    @Provides
    @IntoSet
    override fun provide(): Screen = this
}
