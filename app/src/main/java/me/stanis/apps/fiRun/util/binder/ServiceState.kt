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

package me.stanis.apps.fiRun.util.binder

enum class ServiceState(private val bound: Boolean, private val foregrounded: Boolean) {
    INACTIVE(false, false),
    BOUND(true, false),
    FOREGROUNDED(false, true),
    FOREGROUNDED_AND_BOUND(true, true),
    DESTROYED(false, false);

    fun bind(): ServiceState = if (foregrounded) {
        FOREGROUNDED_AND_BOUND
    } else {
        BOUND
    }

    fun unbind(): ServiceState = if (foregrounded) {
        FOREGROUNDED
    } else {
        INACTIVE
    }

    fun start(): ServiceState = if (bound) {
        FOREGROUNDED_AND_BOUND
    } else {
        FOREGROUNDED
    }

    fun stop(): ServiceState = if (bound) {
        BOUND
    } else {
        INACTIVE
    }

    fun destroy(): ServiceState = DESTROYED
}
