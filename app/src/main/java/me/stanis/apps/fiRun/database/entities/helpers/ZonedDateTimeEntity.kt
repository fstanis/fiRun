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

package me.stanis.apps.fiRun.database.entities.helpers

import androidx.room.Entity
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

@Entity
data class ZonedDateTimeEntity(
    val instant: Instant,
    val zoneId: String
) {
    fun toJava() = ZonedDateTime.ofInstant(instant, ZoneId.of(zoneId))

    companion object {
        fun from(zdt: ZonedDateTime?) =
            zdt?.let { it ->
                ZonedDateTimeEntity(
                    instant = it.toInstant(),
                    zoneId = it.zone.id
                )
            }

        fun from(instant: Instant?) = instant?.let {
            ZonedDateTimeEntity(
                instant = it,
                zoneId = ZoneId.systemDefault().id
            )
        }
    }
}
