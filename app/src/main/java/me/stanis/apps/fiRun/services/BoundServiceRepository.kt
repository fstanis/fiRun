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

package me.stanis.apps.fiRun.services

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

class BoundServiceRepository<T : IBinder>(
    private val type: KClass<out T>,
    private val context: Context,
) {
    private val mutableBinder = MutableStateFlow<T?>(null)
    val binder get() = mutableBinder.asStateFlow()

    suspend fun runWhenConnected(command: T.() -> Unit): Boolean {
        return withTimeoutOrNull(100) {
            binder.filterNotNull().first().command()
        }?.let { true } ?: false
    }

    inline fun <V> flowWhenConnected(crossinline transform: T.() -> Flow<V>) =
        binder.flatMapLatest { it?.let { it.transform() } ?: emptyFlow() }

    private val binderConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            type.safeCast(service)?.also {
                mutableBinder.value = it
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            mutableBinder.value = null
        }
    }

    fun <S : Service> bind(service: KClass<S>) {
        if (binder.value != null) {
            return
        }
        Intent(context, service.java).also { intent ->
            context.bindService(intent, binderConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbind() {
        if (binder.value == null) {
            return
        }
        context.unbindService(binderConnection)
        mutableBinder.value = null
    }

    companion object {
        inline operator fun <reified T : IBinder> invoke(context: Context) =
            BoundServiceRepository(T::class, context)
    }
}
