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

import android.app.Notification
import android.content.Intent
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope

abstract class ForegroundableService<T : IBinder> : LifecycleService() {
    abstract val notification: Notification
    abstract val binder: T
    abstract fun onCoroutineException(throwable: Throwable)

    protected val scope =
        CoroutineScope(
            lifecycleScope.coroutineContext + CoroutineExceptionHandler { _, exception ->
                onCoroutineException(exception)
            }
        )

    protected fun moveToForeground() {
        val intent = Intent(this, this::class.java).also { it.action = ACTION_START_FOREGROUND }
        startForegroundService(intent)
    }

    protected fun removeFromForeground() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    final override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_START_FOREGROUND) {
            startForeground(hashCode(), notification)
            return super.onStartCommand(intent, flags, startId)
        }
        stopSelf()
        return START_NOT_STICKY
    }

    final override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    companion object {
        private const val ACTION_START_FOREGROUND = "start_foreground"
    }
}
