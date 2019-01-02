/*
 * Copyright 2018 Manuel Wrage
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
import com.ivianuu.injekt.GlobalDeclarationRegistry
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.android.androidLogger
import com.ivianuu.injekt.android.applicationComponent
import com.ivianuu.injekt.codegen.Module
import com.ivianuu.injekt.codegen.Single
import com.ivianuu.injekt.configureInjekt
import com.ivianuu.injekt.inject

@Module
private object AutoModule

/**
 * @author Manuel Wrage (IVIanuu)
 */
class App : Application(), InjektTrait {

    override val component by lazy { applicationComponent(this) }

    private val appDependency by inject<AppDependency>()

    override fun onCreate() {
        super.onCreate()

        GlobalDeclarationRegistry.loadModules(autoModule)

        configureInjekt {
            androidLogger()
        }

        appDependency
    }

}

@Single
class AppDependency(val app: App)