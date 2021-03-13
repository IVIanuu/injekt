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

package com.ivianuu.injekt.samples.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.android.ActivityComponent
import com.ivianuu.injekt.android.activityComponent
import com.ivianuu.injekt.component.ComponentElementBinding
import com.ivianuu.injekt.component.element
import com.ivianuu.injekt.samples.android.ui.SampleAppUi
import com.ivianuu.injekt.samples.android.ui.SampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // retrieve our dependencies from the activity component
        val dependencies = activityComponent.element<MainActivityDependencies>()
        // display ui
        setContent {
            dependencies.theme {
                dependencies.appUi()
            }
        }
    }
}

// Declare dependencies we want to retrieve from the activity component
@ComponentElementBinding<ActivityComponent>
@Given
class MainActivityDependencies(
    @Given val theme: SampleTheme,
    @Given val appUi: SampleAppUi
)
