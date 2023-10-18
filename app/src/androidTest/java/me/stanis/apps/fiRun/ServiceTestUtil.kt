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

import android.app.ActivityManager
import android.app.ActivityManager.RunningServiceInfo
import android.app.Instrumentation
import android.app.Service
import android.content.ComponentName
import me.stanis.apps.fiRun.services.exercise.ExerciseService
import me.stanis.apps.fiRun.services.polar.PolarService

class ServiceTestUtil(instrumentation: Instrumentation) {
    private val context = instrumentation.targetContext
    private val activityManager = context.getSystemService(ActivityManager::class.java)

    private val runningServices get() = activityManager.getRunningServices(Int.MAX_VALUE)

    private fun <T : Service> getService(clazz: Class<T>) =
        runningServices.first { it.service == ComponentName(context, clazz) }

    val exerciseServiceInfo: RunningServiceInfo get() = getService(ExerciseService::class.java)
    val polarServiceInfo: RunningServiceInfo get() = getService(PolarService::class.java)
}
