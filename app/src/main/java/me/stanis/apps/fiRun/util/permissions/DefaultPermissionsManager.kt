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

package me.stanis.apps.fiRun.util.permissions

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.flowWithLifecycle
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.combineTransform
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import me.stanis.apps.fiRun.util.permissions.PermissionsChecker.neededPermissions
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultPermissionsManager @Inject constructor(
    @ApplicationContext private val context: Context
) : PermissionsManager {
    private val grantedCategories = MutableStateFlow(currentGrantedCategories)
    private val mutableGrantedPermissions = MutableStateFlow(currentGrantedPermissions)
    private val neededCategories =
        MutableStateFlow<Set<PermissionsChecker.PermissionCategory>>(emptySet())
    private val mutableRequests = MutableSharedFlow<Iterable<String>>(replay = 100)
    private val grantedCategory = PermissionsChecker.PermissionCategory.values()
        .associateWith { MutableStateFlow(currentGrantedCategories.contains(it)) }

    val grantedPermissions get() = mutableGrantedPermissions.asStateFlow()
    val allNeededPermissionsGranted =
        combine(neededCategories, grantedCategories) { requested, granted ->
            granted.containsAll(requested)
        }
    val neededPermissions = neededCategories.map { it.neededPermissions }

    fun permissionRequests(
        permissionRequest: ActivityResultLauncher<Array<String>>
    ) =
        combineTransform(mutableRequests, mutableGrantedPermissions) { requests, granted ->
            val nonGranted = requests.toMutableSet().also { it.removeAll(granted) }
            if (nonGranted.isNotEmpty()) {
                emit(nonGranted.toTypedArray())
            }
        }
            .onEach {
                permissionRequest.launch(it)
            }

    fun launchPermissionRequests(owner: LifecycleOwner, permissionRequest: ActivityResultLauncher<Array<String>>) {
        permissionRequests(permissionRequest).flowWithLifecycle(owner.lifecycle).launchIn(owner.lifecycleScope)
    }

    suspend fun requestPermission(permission: String) {
        mutableRequests.emit(listOf(permission))
    }

    suspend fun requestAllNeededPermissions() {
        mutableRequests.emit(neededPermissions.first())
    }

    override fun requestCategory(category: PermissionsChecker.PermissionCategory) {
        neededCategories.update { categories ->
            categories.toMutableSet().also { it.add(category) }
        }
    }

    suspend fun requestCategoryAndWaitForResult(category: PermissionsChecker.PermissionCategory): Boolean {
        requestCategory(category)
        return combineTransform(neededCategories, grantedCategories) { needed, granted ->
            if (needed.isEmpty()) {
                emit(false)
            } else if (granted.contains(category)) {
                emit(true)
            }
        }.first()
    }

    fun cancelCategoryRequests() {
        neededCategories.value = emptySet()
    }

    fun recheckPermissions() {
        grantedCategories.value = currentGrantedCategories
        for (category in grantedCategories.value) {
            grantedCategory[category]!!.value = true
        }
        mutableGrantedPermissions.value = currentGrantedPermissions
    }

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).also {
            it.data = Uri.fromParts("package", context.packageName, null)
            it.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }

    @Composable
    override fun isGranted(category: PermissionsChecker.PermissionCategory): State<Boolean> =
        grantedCategory[category]?.collectAsState() ?: remember { mutableStateOf(false) }

    private val currentGrantedPermissions get() = PermissionsChecker.grantedPermissions(context)
    private val currentGrantedCategories get() = PermissionsChecker.grantedCategories(context)
}
