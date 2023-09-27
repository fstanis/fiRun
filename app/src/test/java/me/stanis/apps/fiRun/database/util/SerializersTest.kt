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

package me.stanis.apps.fiRun.database.util

import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import me.stanis.apps.fiRun.database.util.serializers.DurationSerializer
import me.stanis.apps.fiRun.database.util.serializers.InstantSerializer
import me.stanis.apps.fiRun.database.util.serializers.UUIDSerializer
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.whenever

class UUIDSerializerTest {
    @Test
    fun `verify UUIDSerializer deserializes serialized input`() {
        val encoder = mock<Encoder>()
        val outputCaptor = argumentCaptor<String>()
        val uuid = UUID.randomUUID()

        val underTest = UUIDSerializer()
        underTest.serialize(encoder, uuid)
        verify(encoder).encodeString(outputCaptor.capture())
        val decoder = mock<Decoder>()
        whenever(decoder.decodeString()).thenReturn(outputCaptor.firstValue)

        assertEquals(uuid, underTest.deserialize(decoder))
    }

    @Test
    fun `verify Duration deserializes serialized input`() {
        val encoder = mock<Encoder>()
        val outputCaptor = argumentCaptor<Long>()
        val duration = Duration.ofMillis(4332)

        val underTest = DurationSerializer()
        underTest.serialize(encoder, duration)
        verify(encoder).encodeLong(outputCaptor.capture())
        val decoder = mock<Decoder>()
        whenever(decoder.decodeLong()).thenReturn(outputCaptor.firstValue)

        assertEquals(duration, underTest.deserialize(decoder))
    }

    @Test
    fun `verify Instant deserializes serialized input`() {
        val encoder = mock<Encoder>()
        val outputCaptor = argumentCaptor<Long>()
        val instant = Instant.ofEpochMilli(1234)

        val underTest = InstantSerializer()
        underTest.serialize(encoder, instant)
        verify(encoder).encodeLong(outputCaptor.capture())
        val decoder = mock<Decoder>()
        whenever(decoder.decodeLong()).thenReturn(outputCaptor.firstValue)

        assertEquals(instant, underTest.deserialize(decoder))
    }
}
