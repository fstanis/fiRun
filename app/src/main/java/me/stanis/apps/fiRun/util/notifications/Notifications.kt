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

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import me.stanis.apps.fiRun.MainActivity
import me.stanis.apps.fiRun.R

@Singleton
class Notifications @Inject constructor(
    @ApplicationContext private val context: Context,
    notificationManager: NotificationManager
) {
    companion object {
        private const val exerciseChannel = "exercise_channel_id"
        private const val polarChannel = "devices_channel_id"
    }

    private val mainActivityPendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java),
        PendingIntent.FLAG_IMMUTABLE
    )

    init {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                "exercise_channel_id",
                context.getString(R.string.notification_channel_exercise),
                NotificationManager.IMPORTANCE_LOW
            )
        )
        notificationManager.createNotificationChannel(
            NotificationChannel(
                "devices_channel_id",
                context.getString(R.string.notification_channel_device),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    fun createPolarNotification(title: String): Notification =
        createNotification(polarChannel, title)

    fun createExerciseNotification(title: String): Notification =
        createNotification(exerciseChannel, title)

    private fun createNotification(channel: String, title: String): Notification {
        return Notification.Builder(context, channel)
            .setContentTitle(title)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(mainActivityPendingIntent)
            .build()
    }
}
