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
import androidx.activity.ComponentActivity
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.android.ApplicationModule

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        appComponent = AppComponentImpl(this)
        appComponent.initializeWorkers()
        appComponent.refreshRepo()
    }
}

lateinit var appComponent: AppComponent

@Component
abstract class AppComponent(@Binding val app: Application) {
    abstract val initializeWorkers: initializeWorkers
    abstract val refreshRepo: refreshRepo
    abstract val mainActivityComponentFactory: (ComponentActivity) -> MainActivityComponent

    @Module protected val applicationModule = ApplicationModule
    @Module protected val dataModule = DataModule
    @Module protected val workerModule = SampleWorkerModule
}
