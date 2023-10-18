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

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

abstract class BindableForegroundService : LifecycleService() {
    val state = MutableStateFlow(ServiceState.INACTIVE)

    private val binder = ServiceBinderConnection.BinderWrapper(this)
    abstract val notification: Notification

    var shouldForeground: Boolean = false
        set(value) {
            if (field == value) return
            field = value
            if (value) {
                val intent =
                    Intent(this, this::class.java).also { it.action = ACTION_START_FOREGROUND }
                startForegroundService(intent)
            } else {
                stop()
            }
        }

    final override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (shouldForeground && intent?.action == ACTION_START_FOREGROUND) {
            startForeground(hashCode(), notification)
            state.update { it.start() }
            return super.onStartCommand(intent, flags, startId)
        }
        stop()
        return START_NOT_STICKY
    }

    final override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        state.update { it.bind() }
        return binder
    }

    final override fun onUnbind(intent: Intent?): Boolean {
        state.update { it.unbind() }
        return super.onUnbind(intent)
    }

    final override fun onDestroy() {
        state.update { it.destroy() }
        super.onDestroy()
    }

    private fun stop() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        state.update { it.stop() }
    }

    companion object {
        private const val ACTION_START_FOREGROUND = "start_foreground"
    }
}
