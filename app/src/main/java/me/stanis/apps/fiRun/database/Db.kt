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

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.stanis.apps.fiRun.database.dao.DeviceDao
import me.stanis.apps.fiRun.database.dao.DistanceDao
import me.stanis.apps.fiRun.database.dao.ExerciseDao
import me.stanis.apps.fiRun.database.dao.HeartRateDao
import me.stanis.apps.fiRun.database.dao.SpeedDao
import me.stanis.apps.fiRun.database.entities.DeviceEntity
import me.stanis.apps.fiRun.database.entities.DistanceEntity
import me.stanis.apps.fiRun.database.entities.ExerciseEntity
import me.stanis.apps.fiRun.database.entities.HeartRateEntity
import me.stanis.apps.fiRun.database.entities.SpeedEntity
import me.stanis.apps.fiRun.database.util.converters.DurationConverters
import me.stanis.apps.fiRun.database.util.converters.HeartRateSourceConverters
import me.stanis.apps.fiRun.database.util.converters.InstantConverters
import me.stanis.apps.fiRun.database.util.converters.ZonedDateTimeConverters

@Database(
    entities = [
        ExerciseEntity::class,
        DistanceEntity::class,
        SpeedEntity::class,
        HeartRateEntity::class,
        DeviceEntity::class
    ],
    version = 11
)
@TypeConverters(
    ZonedDateTimeConverters::class,
    DurationConverters::class,
    InstantConverters::class,
    HeartRateSourceConverters::class
)
abstract class Db : RoomDatabase() {
    abstract fun exercise(): ExerciseDao
    abstract fun distance(): DistanceDao
    abstract fun speed(): SpeedDao
    abstract fun heartRate(): HeartRateDao
    abstract fun devices(): DeviceDao
}
