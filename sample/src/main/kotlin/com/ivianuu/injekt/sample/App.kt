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
import androidx.work.WorkerFactory
import com.ivianuu.injekt.android.CompositionAndroidApp
import com.ivianuu.injekt.inject

@CompositionAndroidApp
class App : Application() {

    private val appServiceRunner: AppServiceRunner by inject()
    private val repo: Repo by inject()
    private val workerFactory: WorkerFactory by inject()

    override fun onCreate() {
        super.onCreate()
        repo.refresh()
        println("injected app $repo")
    }

}
