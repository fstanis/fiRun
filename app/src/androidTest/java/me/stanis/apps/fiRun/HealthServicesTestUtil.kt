package me.stanis.apps.fiRun

import android.content.Context
import android.content.Intent
import me.stanis.apps.fiRun.models.enums.ExerciseType

class HealthServicesTestUtil(private val context: Context) {
    fun enableSynthetic() = sendBroadcast(USE_SYNTHETIC_PROVIDERS)
    fun disableSynthetic() = sendBroadcast(USE_SENSOR_PROVIDERS)
    fun startExercise(exerciseType: ExerciseType) = sendBroadcast(
        when (exerciseType) {
            ExerciseType.IndoorRun -> START_RUNNING_TREADMILL
            ExerciseType.OutdoorRun -> START_RUNNING
        }
    )

    fun stopExercise() = sendBroadcast(STOP_EXERCISE)

    private fun sendBroadcast(action: String) =
        context.sendBroadcast(Intent(action).setPackage(HEALTH_SERVICES_PACKAGE))

    companion object {
        private const val HEALTH_SERVICES_PACKAGE = "com.google.android.wearable.healthservices"
        private const val USE_SYNTHETIC_PROVIDERS = "whs.USE_SYNTHETIC_PROVIDERS"
        private const val USE_SENSOR_PROVIDERS = "whs.USE_SENSOR_PROVIDERS"
        private const val START_RUNNING = "whs.synthetic.user.START_RUNNING"
        private const val START_RUNNING_TREADMILL = "whs.synthetic.user.START_RUNNING_TREADMILL"
        private const val STOP_EXERCISE = "whs.synthetic.user.STOP_EXERCISE"
    }
}
