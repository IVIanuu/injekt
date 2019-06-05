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
import com.ivianuu.injekt.InjektPlugins
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Single
import com.ivianuu.injekt.android.AndroidLogger
import com.ivianuu.injekt.component
import com.ivianuu.injekt.get
import com.ivianuu.injekt.logger
import com.ivianuu.injekt.module

class App : Application(), InjektTrait {

    override val component by lazy { component() }

    override fun onCreate() {
        InjektPlugins.logger = AndroidLogger()

        d { "Injected app dependency ${get<AppDependency>()}" }

        super.onCreate()
    }
}

@Name(PackageName.Companion::class)
annotation class PackageName {
    companion object : Qualifier
}

val appModule = module {
    //    factory<String>(PackageName) { context().packageName }
}

@Single //@ApplicationScope
class AppDependency(val app: App)