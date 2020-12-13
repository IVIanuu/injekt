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
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.setContent
import com.ivianuu.injekt.android.ActivityRetainedStorage
import com.ivianuu.injekt.android.ActivityStorage
import com.ivianuu.injekt.android.ApplicationComponent
import com.ivianuu.injekt.given
import com.ivianuu.injekt.withGiven
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        withGiven(this as ComponentActivity) {
            val keyUis = given<KeyUis>()

            given<ApplicationComponent>()[DummyAppElementKey]()

            given<StorageCoroutineScope<ActivityStorage>>().launch {
                println("Activity work: start")
                try {
                    awaitCancellation()
                } finally {
                    println("Activity work: stop")
                }
            }
            if (savedInstanceState == null) {
                given<StorageCoroutineScope<ActivityRetainedStorage>>().launch {
                    println("Retained work: start")
                    try {
                        awaitCancellation()
                    } finally {
                        println("Retained work: stop")
                    }
                }
            }
            setContent {
                MyAppUi()
            }
        }
    }
}
