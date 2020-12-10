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
import androidx.compose.ui.platform.setContent

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            /*with(activityComponent.get<MainActivityDependencies>()) {
                withMainViewModel {
                    GlobalScope.launch {
                        enqueueWork()
                    }
                }
            }*/
        }
    }
}
/*

@Binding data class MainActivityDependencies(
    val withMainViewModel: WithMainViewModel,
    val enqueueWork: enqueueWork,
)

typealias WithMainViewModel = @Composable (@Composable (MainViewModel) -> Unit) -> Unit

@Binding fun provideWithMainViewModel(viewModelFactory: () -> MainViewModel): WithMainViewModel = {
    val viewModel = remember(viewModelFactory)
    it(viewModel)
}

typealias enqueueWork = () -> Unit

@Binding fun provideEnqueueWork(context: ActivityContext): enqueueWork = {
    WorkManager.getInstance(context)
        .enqueue(
            OneTimeWorkRequestBuilder<TestWorker>()
                .build()
        )
}

@Module val MainViewModelModule = activityViewModel<MainViewModel>()

@Binding class MainViewModel : ViewModel() {
    init {
        println("init")
    }

    override fun onCleared() {
        println("on cleared")
        super.onCleared()
    }
}
*/