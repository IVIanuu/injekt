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

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.Composable
import androidx.compose.remember
import androidx.lifecycle.ViewModel
import androidx.ui.core.setContent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.android.ActivityContext
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.get
import com.ivianuu.injekt.runReader
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            activityComponent.runReader {
                WithMainViewModel {
                    GlobalScope.launch {
                        enqueueWork()
                    }
                }
            }
        }
    }

}

@Reader
@Composable
fun WithMainViewModel(children: @Composable (MainViewModel) -> Unit) {
    val viewModel = remember { get<MainViewModel>() }
    children(viewModel)
}

@Reader
private fun enqueueWork() {
    WorkManager.getInstance(get<ActivityContext>())
        .enqueue(
            OneTimeWorkRequestBuilder<TestWorker>()
                .build()
        )
}

@Reader
class MainViewModel : ViewModel() {
    init {
        println("init ")
    }

    override fun onCleared() {
        println("on cleared")
        super.onCleared()
    }
}
