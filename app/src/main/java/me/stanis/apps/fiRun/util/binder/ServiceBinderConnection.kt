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

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.RetainedLifecycle
import java.io.Closeable
import kotlin.reflect.KClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ServiceBinderConnection<T>(
    private val context: Context
) : ServiceConnection, Closeable, BinderConnection<T>() {
    private val mutableBinder = MutableStateFlow<T?>(null)
    override val binder = mutableBinder.asStateFlow()

    fun unbind() {
        mutableBinder.value = null
        context.unbindService(this)
    }

    override fun close() = unbind()

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        mutableBinder.value = ((service as BinderWrapper<*>).binder as T)
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        mutableBinder.value = null
    }

    class BinderWrapper<T>(val binder: T) : Binder()

    companion object {
        inline fun <reified S : Service> RetainedLifecycle.bindService(
            context: Context
        ): ServiceBinderConnection<S> {
            val connection = ServiceBinderConnection<S>(context)
            bindService(context, S::class, connection)
            addOnClearedListener {
                connection.unbind()
            }
            return connection
        }

        inline fun <reified S : Service> ViewModel.bindService(
            context: Context
        ): ServiceBinderConnection<S> {
            val connection = ServiceBinderConnection<S>(context)
            bindService(context, S::class, connection)
            addCloseable(connection)
            return connection
        }

        fun <S : Service> bindService(
            context: Context,
            service: KClass<S>,
            connection: ServiceBinderConnection<*>
        ) {
            Intent(context, service.java).also { intent ->
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        }
    }
}
