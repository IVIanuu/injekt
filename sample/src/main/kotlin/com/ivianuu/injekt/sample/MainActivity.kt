/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.sample

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.compose.remember
import androidx.lifecycle.ViewModel
import androidx.ui.core.setContent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.Scoped
import com.ivianuu.injekt.android.ActivityComponent
import com.ivianuu.injekt.android.ActivityViewModel
import com.ivianuu.injekt.android.ForActivity
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.Readable
import com.ivianuu.injekt.get
import com.ivianuu.injekt.composition.runReading
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            activityComponent.runReading {
                get<ActivityScopedStuff>()
                WithMainViewModel {
                    GlobalScope.launch {
                        enqueueWork()
                    }
                }
            }
        }
    }
}

@Scoped<ActivityComponent>
class ActivityScopedStuff

@Readable
@Composable
fun WithMainViewModel(children: @Composable (MainViewModel) -> Unit) {
    val viewModel = remember { get<MainViewModel>() }
    children(viewModel)
}

@Readable
private fun enqueueWork() {
    WorkManager.getInstance(get<@ForActivity Context>())
        .enqueue(
            OneTimeWorkRequestBuilder<TestWorker>()
                .build()
        )
}

@ActivityViewModel
class MainViewModel(private val repo: Repo) : ViewModel() {
    init {
        println("init ")
    }

    override fun onCleared() {
        println("on cleared")
        super.onCleared()
    }
}
