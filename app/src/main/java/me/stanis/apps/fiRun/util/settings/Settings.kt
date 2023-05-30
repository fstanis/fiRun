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

package me.stanis.apps.fiRun.util.settings

import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import me.stanis.apps.fiRun.database.Db
import me.stanis.apps.fiRun.database.entities.Setting
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class Settings @Inject constructor(db: Db) {
    private val dao = db.settings()

    private suspend fun getSetting(key: String): Setting = dao.get(key) ?: Setting("")

    suspend fun getBoolean(key: Key) = getSetting(key.keyStr).booleanValue
    suspend fun getString(key: Key) = getSetting(key.keyStr).stringValue
    suspend fun getInt(key: Key) = getSetting(key.keyStr).intValue
    suspend fun getFloat(key: Key) = getSetting(key.keyStr).floatValue

    fun observeBoolean(key: Key) =
        dao.observe(key.keyStr).filterNotNull().map { it.booleanValue }

    fun observeString(key: Key) = dao.observe(key.keyStr).filterNotNull().map { it.stringValue }
    fun observeInt(key: Key) = dao.observe(key.keyStr).filterNotNull().map { it.intValue }
    fun observeFloat(key: Key) = dao.observe(key.keyStr).filterNotNull().map { it.floatValue }

    suspend fun <T> put(key: Key, value: T) {
        when (value) {
            is Boolean -> dao.put(Setting(key = key.keyStr, booleanValue = value))
            is String -> dao.put(Setting(key = key.keyStr, stringValue = value))
            is Int -> dao.put(Setting(key = key.keyStr, intValue = value))
            is Float -> dao.put(Setting(key = key.keyStr, floatValue = value))
        }
    }

    suspend fun remove(key: String) {
        dao.remove(Setting(key))
    }

    companion object {
        enum class Key(internal val keyStr: String) {
            DEVICE_ID("device_id"),
            ONLY_HIGH_HR_ACCURACY("only_high_hr_accuracy");

            override fun toString(): String = keyStr
        }
    }
}
