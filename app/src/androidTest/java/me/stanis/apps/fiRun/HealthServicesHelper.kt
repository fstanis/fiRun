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

import android.Manifest
import android.app.Instrumentation
import android.content.Intent
import android.provider.Settings
import me.stanis.apps.fiRun.models.enums.ExerciseType

class HealthServicesHelper(instrumentation: Instrumentation) {
    private val context = instrumentation.targetContext
    private val uiAutomation = instrumentation.uiAutomation

    fun enableSynthetic() {
        enableDeveloperSettings()
        sendBroadcast(USE_SYNTHETIC_PROVIDERS)
    }

    fun disableSynthetic() = sendBroadcast(USE_SENSOR_PROVIDERS)
    fun startExercise(exerciseType: ExerciseType) = sendBroadcast(
        when (exerciseType) {
            ExerciseType.IndoorRun -> START_RUNNING_TREADMILL
            ExerciseType.OutdoorRun -> START_RUNNING
        }
    )

    fun stopExercise() = sendBroadcast(STOP_EXERCISE)

    private fun sendBroadcast(action: String) =
        context.sendBroadcast(Intent(action).setPackage(HEALTH_SERVICES_PACKAGE))

    private fun enableDeveloperSettings() {
        uiAutomation.adoptShellPermissionIdentity(Manifest.permission.WRITE_SECURE_SETTINGS)
        Settings.Global.putInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            1
        )
        uiAutomation.dropShellPermissionIdentity()
    }

    companion object {
        private const val HEALTH_SERVICES_PACKAGE = "com.google.android.wearable.healthservices"
        private const val USE_SYNTHETIC_PROVIDERS = "whs.USE_SYNTHETIC_PROVIDERS"
        private const val USE_SENSOR_PROVIDERS = "whs.USE_SENSOR_PROVIDERS"
        private const val START_RUNNING = "whs.synthetic.user.START_RUNNING"
        private const val START_RUNNING_TREADMILL = "whs.synthetic.user.START_RUNNING_TREADMILL"
        private const val STOP_EXERCISE = "whs.synthetic.user.STOP_EXERCISE"
    }
}
