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

@file:OptIn(ExperimentalSerializationApi::class)

package me.stanis.apps.fiRun.database.util.serializers

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import java.io.InputStream
import java.io.OutputStream
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.cbor.Cbor

class CborSerializer<T>(
    override val defaultValue: T,
    private val serializer: KSerializer<T>
) : Serializer<T> {
    override suspend fun readFrom(input: InputStream): T {
        try {
            return Cbor.decodeFromByteArray(serializer, input.readBytes())
        } catch (exception: SerializationException) {
            throw CorruptionException("Unable to read from data store", exception)
        }
    }

    override suspend fun writeTo(t: T, output: OutputStream) {
        output.write(Cbor.encodeToByteArray(serializer, t))
    }
}
