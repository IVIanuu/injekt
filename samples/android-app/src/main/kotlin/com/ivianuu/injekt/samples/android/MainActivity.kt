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

package com.ivianuu.injekt.samples.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.setContent
import androidx.lifecycle.ViewModel
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ivianuu.injekt.Assisted
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.android.ActivityComponent
import com.ivianuu.injekt.android.ActivityContext
import com.ivianuu.injekt.android.GivenActivityViewModel
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.merge.EntryPoint
import com.ivianuu.injekt.merge.entryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            with(activityComponent.entryPoint<MainActivityDependencies>()) {
                WithMainViewModel {
                    GlobalScope.launch {
                        enqueueWork()
                    }
                }
            }
        }
    }

}

@EntryPoint(ActivityComponent::class)
interface MainActivityDependencies {
    val WithMainViewModel: WithMainViewModel
    val enqueueWork: enqueueWork
}

@Given
@Composable
fun WithMainViewModel(
    viewModelFactory: () -> MainViewModel,
    @Assisted children: @Composable (MainViewModel) -> Unit,
) {
    val viewModel = remember { viewModelFactory() }
    children(viewModel)
}

@Given
fun enqueueWork(context: ActivityContext) {
    WorkManager.getInstance(context)
        .enqueue(
            OneTimeWorkRequestBuilder<TestWorker>()
                .build()
        )
}

@GivenActivityViewModel
class MainViewModel : ViewModel() {
    init {
        println("init")
    }

    override fun onCleared() {
        println("on cleared")
        super.onCleared()
    }
}
