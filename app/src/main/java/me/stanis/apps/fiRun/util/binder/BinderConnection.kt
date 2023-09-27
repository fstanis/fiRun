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

@file:OptIn(ExperimentalCoroutinesApi::class)

package me.stanis.apps.fiRun.util.binder

import android.os.IBinder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest

abstract class BinderConnection<T : IBinder> {
    abstract val binder: StateFlow<T?>

    suspend fun <R> runWhenConnected(command: suspend (T) -> R): R =
        command(binder.filterNotNull().first())

    fun <N, V : Flow<N>> flowWhenConnected(flow: T.() -> V) =
        binder.flatMapLatest { it?.flow() ?: emptyFlow() }
}
