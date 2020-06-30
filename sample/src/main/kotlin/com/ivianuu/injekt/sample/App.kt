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

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import com.ivianuu.injekt.android.applicationComponent
import com.ivianuu.injekt.composition.get
import com.ivianuu.injekt.composition.initializeCompositions

class App : Application() {

    private val appServiceRunner: AppServiceRunner by lazy { applicationComponent.get() }
    private val repo: Repo by lazy { applicationComponent.get() }
    private val workerFactory: WorkerFactory by lazy { applicationComponent.get() }

    override fun onCreate() {
        initializeCompositions()
        super.onCreate()
        repo.refresh()

        WorkManager.initialize(
            this, Configuration.Builder()
                .setWorkerFactory(applicationComponent.get()).build()
        )

        println("injected app $repo")
    }

}
