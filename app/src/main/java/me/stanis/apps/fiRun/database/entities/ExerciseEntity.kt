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
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Duration
import java.util.UUID
import me.stanis.apps.fiRun.database.entities.helpers.ZonedDateTimeEntity
import me.stanis.apps.fiRun.models.enums.ExerciseType

@Entity(tableName = "exercise")
data class ExerciseEntity(
    @PrimaryKey val exerciseId: UUID,
    @ColumnInfo(defaultValue = "") val title: String,
    val type: ExerciseType,
    @Embedded(prefix = "start_") val startTime: ZonedDateTimeEntity?,
    @Embedded(prefix = "end_") val endTime: ZonedDateTimeEntity?,
    val duration: Duration?
)

data class ExerciseEntityCreate(val exerciseId: UUID, val type: ExerciseType)
data class ExerciseEntityUpdateStart(
    val exerciseId: UUID,
    @Embedded(prefix = "start_") val startTime: ZonedDateTimeEntity?,
    val duration: Duration?
)

data class ExerciseEntityUpdateEnd(
    val exerciseId: UUID,
    @Embedded(prefix = "end_") val endTime: ZonedDateTimeEntity?,
    val duration: Duration?
)

data class ExerciseWithData(
    @Embedded val exerciseEntity: ExerciseEntity,

    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "exerciseId"
    )
    val distanceEntities: List<DistanceEntity>,

    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "exerciseId"
    )
    val heartRateEntities: List<HeartRateEntity>,

    @Relation(
        parentColumn = "exerciseId",
        entityColumn = "exerciseId"
    )
    val speedEntities: List<SpeedEntity>
)
