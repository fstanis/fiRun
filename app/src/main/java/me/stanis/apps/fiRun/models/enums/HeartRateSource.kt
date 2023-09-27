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

package me.stanis.apps.fiRun.models.enums

sealed interface HeartRateSource {
    override fun toString(): String

    object Unknown : HeartRateSource {
        override fun toString() = UNKNOWN_STRING
    }

    object Internal : HeartRateSource {
        override fun toString() = INTERNAL_STRING
    }

    data class External(val deviceId: String) : HeartRateSource {
        override fun toString() = "$EXTERNAL_PREFIX$deviceId"
    }

    companion object {
        private const val UNKNOWN_STRING = "[UNKNOWN]"
        private const val INTERNAL_STRING = "[INTERNAL]"
        private const val EXTERNAL_PREFIX = "[EXTERNAL] "

        fun fromString(str: String?): HeartRateSource =
            when {
                str == null -> Unknown
                str == INTERNAL_STRING -> Internal
                str.startsWith(EXTERNAL_PREFIX) -> External(str.removePrefix(EXTERNAL_PREFIX))
                else -> Unknown
            }
    }
}
