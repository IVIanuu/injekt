/*
 * Copyright 2021 Manuel Wrage
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

@file:GivenImports("com.ivianuu.injekt.android.*")

package com.ivianuu.injekt.samples.android.app

import android.os.*
import androidx.activity.*
import androidx.activity.compose.*
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.coroutines.*
import com.ivianuu.injekt.samples.android.ui.*
import com.ivianuu.injekt.scope.*
import kotlinx.coroutines.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // retrieve our dependencies from the activity scope
        val dependencies = element(typeKeyOf<MainActivityDependencies>())
        coroutineScope().launch {

        }
        // display ui
        setContent {
            dependencies.theme {
                dependencies.appUi()
            }
        }
    }
}

// Declare dependencies we want to retrieve from the activity scope
@Given
@InstallElement<ActivityGivenScope>
class MainActivityDependencies(
    @Given val theme: SampleTheme,
    @Given val appUi: SampleAppUi
)
