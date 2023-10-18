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

package me.stanis.apps.fiRun.database

import android.content.Context
import androidx.datastore.dataStore
import kotlinx.serialization.KSerializer
import me.stanis.apps.fiRun.database.datastore.CurrentExerciseData
import me.stanis.apps.fiRun.database.datastore.SettingsData
import me.stanis.apps.fiRun.database.util.serializers.CborSerializer

object DataStores {
    fun settingsData(context: Context) = context.settingsData
    fun currentExerciseData(context: Context) = context.currentExerciseData

    private val Context.settingsData by createDelegate(
        SettingsData.EMPTY,
        SettingsData.serializer()
    )
    private val Context.currentExerciseData by createDelegate(
        CurrentExerciseData.EMPTY,
        CurrentExerciseData.serializer()
    )

    private inline fun <reified T> createDelegate(
        defaultValue: T,
        serializer: KSerializer<T>
    ) = dataStore(T::class.simpleName!!, CborSerializer(defaultValue, serializer))
}
