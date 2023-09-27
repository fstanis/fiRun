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

package me.stanis.apps.fiRun.modules

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.ActivityRetainedLifecycle
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import me.stanis.apps.fiRun.services.exercise.ExerciseBinder
import me.stanis.apps.fiRun.services.exercise.ExerciseService
import me.stanis.apps.fiRun.services.polar.PolarBinder
import me.stanis.apps.fiRun.services.polar.PolarService
import me.stanis.apps.fiRun.util.binder.BinderConnection
import me.stanis.apps.fiRun.util.binder.ServiceBinderConnection.Companion.bindService

@Module
@InstallIn(ActivityRetainedComponent::class)
internal object BindersModule {
    @ActivityRetainedScoped
    @Provides
    fun providePolarBinder(
        @ApplicationContext context: Context,
        lifecycle: ActivityRetainedLifecycle
    ): BinderConnection<PolarBinder> = lifecycle.bindService<PolarBinder, PolarService>(
        context
    )

    @ActivityRetainedScoped
    @Provides
    fun provideExerciseBinder(
        @ApplicationContext context: Context,
        lifecycle: ActivityRetainedLifecycle
    ): BinderConnection<ExerciseBinder> = lifecycle.bindService<ExerciseBinder, ExerciseService>(
        context
    )
}
