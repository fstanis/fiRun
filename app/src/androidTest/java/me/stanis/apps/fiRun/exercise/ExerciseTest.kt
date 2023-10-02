@file:OptIn(ExperimentalTestApi::class)

package me.stanis.apps.fiRun.exercise

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.datastore.core.DataStore
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runTest
import me.stanis.apps.fiRun.HealthServicesTestUtil
import me.stanis.apps.fiRun.MainActivity
import me.stanis.apps.fiRun.database.dao.ExerciseDao
import me.stanis.apps.fiRun.database.datastore.CurrentExerciseData
import me.stanis.apps.fiRun.models.enums.ExerciseType
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker.neededPermissions
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@HiltAndroidTest
class ExerciseTest {
    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val rule = GrantPermissionRule.grant(
        *setOf(PermissionsChecker.PermissionCategory.INDOOR_RUN).neededPermissions.toTypedArray()
    )

    @get:Rule(order = 2)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val hsTestUtil = HealthServicesTestUtil(instrumentation.targetContext)

    @Inject
    lateinit var currentExerciseData: DataStore<CurrentExerciseData>

    @Inject
    lateinit var exerciseDao: ExerciseDao

    @Before
    fun setUp() {
        hiltRule.inject()
        hsTestUtil.enableSynthetic()
    }

    @After
    fun tearDown() {
        hsTestUtil.disableSynthetic()
    }

    @Test
    fun startingExerciseSetsExerciseId() = runTest {
        val currentExercise = currentExerciseData.data.stateIn(backgroundScope)
        composeTestRule.waitUntilDoesNotExist(hasTestTag("loading"))

        assertNull(currentExercise.value.id)
        composeTestRule.onNodeWithText("Indoor run").performClick()
        composeTestRule.awaitIdle()

        assertNotNull(currentExercise.value.id)
    }

    @Test
    fun exerciseAddsDataToRoom() = runTest(timeout = 30.seconds) {
        hsTestUtil.startExercise(ExerciseType.IndoorRun)
        composeTestRule.waitUntilDoesNotExist(hasTestTag("loading"))
        composeTestRule.onNodeWithText("Indoor run").performClick()
        val currentExerciseId = currentExerciseData.data.mapNotNull { it.id }.first()

        composeTestRule.waitUntilAtLeastOneExists(hasText("00:00", true))
        var data = exerciseDao.getWithData(currentExerciseId)
        assertNotNull(data)
        assertNotNull(data.exerciseEntity.startTime)
        assertNull(data.exerciseEntity.endTime)
        assertEquals(ExerciseType.IndoorRun, data.exerciseEntity.type)

        composeTestRule.waitUntilAtLeastOneExists(
            hasText("00:10", true),
            20.seconds.inWholeMilliseconds
        )
        composeTestRule.onNodeWithContentDescription("Pause exercise").performClick()
        composeTestRule.onNodeWithContentDescription("End exercise").performClick()
        composeTestRule.awaitIdle()

        data = exerciseDao.getWithData(currentExerciseId)
        assertNotNull(data)
        assertNotNull(data.exerciseEntity.startTime)
        assertNotNull(data.exerciseEntity.endTime)
        assertTrue(data.distanceEntities.isNotEmpty())
        assertTrue(data.heartRateEntities.isNotEmpty())
        assertTrue(data.speedEntities.isNotEmpty())
        hsTestUtil.stopExercise()
    }
}
