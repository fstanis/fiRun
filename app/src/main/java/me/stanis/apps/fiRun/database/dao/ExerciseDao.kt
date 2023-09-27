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

package me.stanis.apps.fiRun.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import me.stanis.apps.fiRun.database.entities.ExerciseEntity
import me.stanis.apps.fiRun.database.entities.ExerciseEntityCreate
import me.stanis.apps.fiRun.database.entities.ExerciseEntityUpdateEnd
import me.stanis.apps.fiRun.database.entities.ExerciseEntityUpdateStart
import me.stanis.apps.fiRun.database.entities.ExerciseWithData

@Dao
interface ExerciseDao {
    @Insert(entity = ExerciseEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun create(partial: ExerciseEntityCreate): Long

    @Update(entity = ExerciseEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateStart(partial: ExerciseEntityUpdateStart)

    @Update(entity = ExerciseEntity::class, onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateEnd(partial: ExerciseEntityUpdateEnd)

    @Update
    suspend fun update(entity: ExerciseEntity)

    @Query("SELECT * FROM exercise WHERE [exerciseId] = :exerciseId LIMIT 1")
    suspend fun get(exerciseId: UUID): ExerciseEntity

    @Query("SELECT * FROM exercise WHERE [rowid] = :rowId LIMIT 1")
    suspend fun getFromRow(rowId: Long): ExerciseEntity

    @Query("SELECT * FROM exercise WHERE [exerciseId] = :exerciseId LIMIT 1")
    fun observe(exerciseId: UUID): Flow<ExerciseEntity?>

    @Transaction
    @Query("SELECT * FROM exercise WHERE [exerciseId] = :exerciseId LIMIT 1")
    suspend fun getWithData(exerciseId: UUID): ExerciseWithData?

    @Transaction
    @Query("SELECT * FROM exercise ORDER BY start_instant DESC LIMIT 1")
    suspend fun getLastWithData(): ExerciseWithData?
}
