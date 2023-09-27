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

package me.stanis.apps.fiRun.ui.summary

import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.stanis.apps.fiRun.database.dao.ExerciseDao
import me.stanis.apps.fiRun.database.entities.ExerciseWithData
import me.stanis.apps.fiRun.persistence.ExerciseManager
import me.stanis.apps.fiRun.ui.BaseViewModel

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val exerciseDao: ExerciseDao,
    private val exerciseManager: ExerciseManager
) : BaseViewModel() {
    private val mutableExerciseData = MutableStateFlow<ExerciseWithData?>(null)
    val exerciseData get() = mutableExerciseData.asStateFlow()

    init {
        refreshLatest()
    }

    fun refreshLatest() {
        viewModelScope.launch {
            mutableExerciseData.value = exerciseDao.getLastWithData()
        }
    }

    val status = exerciseManager.exerciseState
}
