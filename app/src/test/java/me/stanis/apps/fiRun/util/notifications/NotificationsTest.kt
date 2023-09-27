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

package me.stanis.apps.fiRun.util.notifications

import android.app.Notification.EXTRA_TITLE
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import kotlin.test.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NotificationsTest {
    private val context: Context = getApplicationContext()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val shadowNotificationManager = shadowOf(notificationManager)

    @Test
    fun `constructor creates notification channels`() {
        assertEquals(emptyList(), shadowNotificationManager.notificationChannels)

        Notifications(context, notificationManager)

        assertEquals(
            listOf("devices_channel_id", "exercise_channel_id"),
            shadowNotificationManager.notificationChannels.map { (it as NotificationChannel).id }
        )
    }

    @Test
    fun `createPolarNotification creates notification`() {
        val underTest = Notifications(context, notificationManager).createPolarNotification("title")

        assertEquals("devices_channel_id", underTest.channelId)
        assertEquals("title", underTest.extras.getCharSequence(EXTRA_TITLE))
    }

    @Test
    fun `createExerciseNotification creates notification`() {
        val underTest =
            Notifications(context, notificationManager).createExerciseNotification("title")

        assertEquals("exercise_channel_id", underTest.channelId)
        assertEquals("title", underTest.extras.getCharSequence(EXTRA_TITLE))
    }
}
