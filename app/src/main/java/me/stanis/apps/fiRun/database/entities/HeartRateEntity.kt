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

package me.stanis.apps.fiRun.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.time.Duration
import java.time.Instant
import java.util.UUID
import me.stanis.apps.fiRun.models.enums.HeartRateAccuracy
import me.stanis.apps.fiRun.models.enums.HeartRateSource

@Entity(
    tableName = "heartRate",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = arrayOf("exerciseId"),
            childColumns = arrayOf("exerciseId"),
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HeartRateEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(index = true) val exerciseId: UUID,
    val bpm: Int,
    val accuracy: HeartRateAccuracy,
    val source: HeartRateSource,
    val instant: Instant,
    val exerciseDuration: Duration
)
