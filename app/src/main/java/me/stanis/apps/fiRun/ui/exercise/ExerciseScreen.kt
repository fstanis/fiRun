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

package me.stanis.apps.fiRun.ui.exercise

import android.annotation.SuppressLint
import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.stanis.apps.fiRun.models.HeartRate
import me.stanis.apps.fiRun.models.enums.ExerciseStatus
import me.stanis.apps.fiRun.ui.Screen
import me.stanis.apps.fiRun.ui.home.HomeScreen
import me.stanis.apps.fiRun.ui.navigate
import me.stanis.apps.fiRun.ui.popUpTo
import me.stanis.apps.fiRun.ui.summary.SummaryScreen
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

@Module
@InstallIn(SingletonComponent::class)
object ExerciseScreen : Screen {
    @SuppressLint("ComposableNaming")
    @Composable
    override fun content(navController: NavHostController, navEntry: NavBackStackEntry) {
        val viewModel = hiltViewModel<ExerciseViewModel>()
        val uiState by viewModel.uiStateFlow.collectAsState()
        val latestStats by viewModel.latestStatsFlow.collectAsState()
        val keepScreenOn by viewModel.keepScreenOn.collectAsState()
        when (uiState.exerciseState.status) {
            ExerciseStatus.InProgress -> ExercisePage(
                modifier = Modifier.fillMaxSize(),
                heartRate = latestStats.exerciseHr,
                polarHr = latestStats.polarHr,
                distance = latestStats.distance?.total,
                durationProducer = uiState.exerciseState::getDuration,
                averagePace = latestStats.averagePace?.average,
                currentPace = latestStats.currentPace?.current,
                calories = latestStats.calories?.total,
                keepScreenOn = keepScreenOn,
                onPauseExerciseClick = viewModel::pauseExercise
            )

            ExerciseStatus.Paused -> {
                PausedPage(
                    modifier = Modifier.fillMaxSize(),
                    distance = latestStats.distance?.total ?: 0.0,
                    duration = uiState.exerciseState.getDuration(Instant.now()) ?: Duration.ZERO,
                    onUnpauseExerciseClick = {
                        viewModel.resumeExercise()
                    },
                    onEndExerciseClick = {
                        viewModel.endExercise()
                    }
                )
            }

            ExerciseStatus.Loading ->
                LoadingPage(Modifier.fillMaxSize())

            else -> navController.navigate(SummaryScreen) {
                popUpTo(HomeScreen)
            }
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
    private fun ExercisePage(
        modifier: Modifier,
        heartRate: HeartRate?,
        polarHr: HeartRate?,
        distance: Double?,
        averagePace: Duration?,
        currentPace: Duration?,
        calories: Int?,
        durationProducer: (Instant) -> Duration?,
        keepScreenOn: Boolean,
        onPauseExerciseClick: () -> Unit
    ) {
        val duration by produceState(durationProducer(Instant.now())) {
            while (isActive) {
                value = durationProducer(Instant.now())
                delay(1000)
            }
        }
        if (keepScreenOn) {
            KeepScreenOn()
        }
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("❤ ${heartRate?.bpm ?: "…"} (${polarHr?.bpm ?: "…"})")
            Text(
                "\uD83C\uDFC3 ${currentPace?.formatAsPace() ?: "…"} (${averagePace?.formatAsPace() ?: "…"})"
            )
            Text("\uD83C\uDF82 ${calories ?: "…"}")
            Text("\uD83D\uDCCF ${formatDistance(distance)}")
            Text("⏲ ${duration.formatElapsedTime()}")
            Button(onClick = onPauseExerciseClick) {
                Icon(imageVector = Icons.Outlined.Pause, contentDescription = null)
            }
        }
    }

    @Composable
    fun PausedPage(
        modifier: Modifier,
        distance: Double,
        duration: Duration,
        onUnpauseExerciseClick: () -> Unit,
        onEndExerciseClick: () -> Unit
    ) {
        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Text("\uD83D\uDCCF ${String.format("%.2f", distance / 1000)}km")
            Text("⏲ ${duration.formatElapsedTime()}")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                Button(onClick = onUnpauseExerciseClick) {
                    Icon(imageVector = Icons.Outlined.PlayArrow, contentDescription = null)
                }
                Button(onClick = onEndExerciseClick) {
                    Icon(imageVector = Icons.Outlined.Stop, contentDescription = null)
                }
            }
        }
    }

    @Composable
    fun KeepScreenOn() {
        val currentView = LocalView.current
        DisposableEffect(Unit) {
            currentView.keepScreenOn = true
            onDispose {
                currentView.keepScreenOn = false
            }
        }
    }

    private fun Duration.formatAsPace() = toKotlinDuration().toComponents { minutes, seconds, _ ->
        String.format(
            "%d:%02d",
            minutes,
            seconds
        )
    }

    private fun Duration?.formatElapsedTime() =
        this?.let { DateUtils.formatElapsedTime(it.seconds) } ?: "…"

    private fun formatDistance(distance: Double?) =
        if (distance == null) {
            "…"
        } else if (distance <= 500) {
            String.format("%.0f m", distance)
        } else {
            String.format("%.2f km", distance / 1000)
        }

    @Provides
    @IntoSet
    override fun provide(): Screen = this
}
