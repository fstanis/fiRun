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

package me.stanis.apps.fiRun.services.exercise

import androidx.health.services.client.ExerciseClient
import androidx.health.services.client.ExerciseUpdateCallback
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.job
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.services.exercise.ExerciseCallback.Companion.setExerciseUpdateCallback
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExerciseCallbackTest {
    private val client: ExerciseClient = mock()
    private val callbackCaptor = argumentCaptor<ExerciseUpdateCallback>()

    @Test
    fun `onRegistered resumes continuation`() = runTest {
        val task = async { client.setExerciseUpdateCallback {} }
        runCurrent()
        verify(client).setUpdateCallback(callbackCaptor.capture())
        val callback = callbackCaptor.firstValue

        callback.onRegistered()
        runCurrent()
        assertEquals(callback, task.getCompleted())
    }

    @Test
    fun `onRegistrationFailed fails continuation`() = runTest {
        val task =
            async(SupervisorJob(backgroundScope.coroutineContext.job)) { client.setExerciseUpdateCallback {} }
        runCurrent()
        verify(client).setUpdateCallback(callbackCaptor.capture())
        val callback = callbackCaptor.firstValue
        val throwable = Throwable()

        callback.onRegistrationFailed(throwable)
        runCurrent()
        assertEquals(throwable, task.getCompletionExceptionOrNull())
    }
}
