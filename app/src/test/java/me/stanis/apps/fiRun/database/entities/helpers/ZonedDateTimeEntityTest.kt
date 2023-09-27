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

import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.assertEquals
import org.junit.Test

class ZonedDateTimeEntityTest {
    @Test
    fun `toJava returns valid ZonedDateTime`() {
        val underTest = ZonedDateTimeEntity(
            instant = Instant.parse("2023-09-20T12:00:00Z"),
            zoneId = "UTC"
        ).toJava()

        assertEquals(ZonedDateTime.parse("2023-09-20T12:00:00Z[UTC]"), underTest)
    }

    @Test
    fun `from for given ZonedDateTime returns valid entity`() {
        val underTest = ZonedDateTimeEntity.from(ZonedDateTime.parse("2023-09-20T12:00:00Z[UTC]"))

        assertEquals(Instant.parse("2023-09-20T12:00:00Z"), underTest?.instant)
        assertEquals("UTC", underTest?.zoneId)
    }

    @Test
    fun `from for given Instant returns valid entity`() {
        val underTest = ZonedDateTimeEntity.from(Instant.parse("2023-09-20T12:00:00Z"))

        assertEquals(Instant.parse("2023-09-20T12:00:00Z"), underTest?.instant)
        assertEquals(ZoneId.systemDefault().id, underTest?.zoneId)
    }
}
