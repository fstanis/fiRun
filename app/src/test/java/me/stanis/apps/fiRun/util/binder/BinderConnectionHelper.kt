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

import android.app.Application
import android.content.ComponentName
import android.os.IBinder
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import me.stanis.apps.fiRun.services.ForegroundableService
import me.stanis.apps.fiRun.util.binder.ServiceBinderConnection.Companion.bindService
import org.robolectric.Robolectric.setupService
import org.robolectric.Shadows.shadowOf

object BinderConnectionHelper {
    inline fun <reified T : IBinder, reified S : ForegroundableService<T>> createBinderConnection(): ServiceBinderConnection<T> {
        val app: Application = getApplicationContext()
        val shadowApp = shadowOf(app)
        val service = setupService(S::class.java)
        shadowApp.setBindServiceCallsOnServiceConnectedDirectly(true)
        shadowApp.setComponentNameAndServiceForBindService(
            ComponentName(app, S::class.java),
            service.binder
        )
        return ServiceBinderConnection(app, T::class).also { bindService(app, S::class, it) }
    }
}
