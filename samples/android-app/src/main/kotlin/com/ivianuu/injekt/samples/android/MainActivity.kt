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
import androidx.activity.compose.setContent
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.android.ActivityComponent
import com.ivianuu.injekt.android.ActivityRetainedComponent
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.component.ComponentElementBinding
import com.ivianuu.injekt.component.element
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val component = activityComponent.element<MainActivityComponent>()
        component.activityScope.launch {
            println("Activity work: start")
            try {
                awaitCancellation()
            } finally {
                println("Activity work: stop")
            }
        }
        if (savedInstanceState == null) {
            component.retainedActivityScope.launch {
                println("Retained work: start")
                try {
                    awaitCancellation()
                } finally {
                    println("Retained work: stop")
                }
            }
        }
        setContent { component.homeUi() }
    }
}

@ComponentElementBinding<ActivityComponent>
@Given
class MainActivityComponent(
    @Given val activityScope: ComponentCoroutineScope<ActivityComponent>,
    @Given val retainedActivityScope: ComponentCoroutineScope<ActivityRetainedComponent>,
    @Given keyUis: Set<KeyUiElement>
) {
    val homeUi = keyUis.toMap()[CounterKey::class]!!
}
