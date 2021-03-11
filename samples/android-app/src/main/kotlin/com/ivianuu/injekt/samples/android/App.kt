// injekt-incremental-fix 1615499624200 injekt-end
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

import android.app.Application
import androidx.work.WorkerFactory
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.android.AppContext
import com.ivianuu.injekt.component.AppComponent
import com.ivianuu.injekt.component.ComponentInitializer
import com.ivianuu.injekt.component.ComponentInitializerBinding
import com.ivianuu.injekt.component.initializeApp
import com.ivianuu.injekt.component.*
import com.ivianuu.injekt.common.*
import com.ivianuu.injekt.android.*
import com.ivianuu.injekt.android.work.*

class App : Application() {
    override fun onCreate() {
        initializeApp()
        super.onCreate()
    }
}

@ComponentInitializerBinding
@Given
fun myAppComponentInitializer(
    @Given appContext: AppContext,
    @Given database: Database,
    @Given workerFactory: WorkerFactory
): ComponentInitializer<AppComponent> = {
    initializeWorkers()
}
