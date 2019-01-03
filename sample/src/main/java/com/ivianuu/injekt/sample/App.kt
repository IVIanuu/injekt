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
import androidx.lifecycle.GenericLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.android.androidLogger
import com.ivianuu.injekt.android.applicationComponent
import com.ivianuu.injekt.codegen.Module
import com.ivianuu.injekt.codegen.Single
import com.ivianuu.injekt.common.instanceModule
import com.ivianuu.injekt.configureInjekt
import com.ivianuu.injekt.inject

lateinit var appComponent: Component

interface AppComponentTrait : InjektTrait {
    override val component: Component
        get() = appComponent
}

/**
 * @author Manuel Wrage (IVIanuu)
 */
class App : Application(), AppComponentTrait {

    private val appDependency by inject<AppDependency>()

    override fun onCreate() {
        super.onCreate()

        configureInjekt {
            androidLogger()
        }

        appComponent = applicationComponent(this) { modules(appModule, instanceModule(this)) }

        appDependency
    }

}

fun Component.scopedModules(key: String, vararg modules: com.ivianuu.injekt.Module, dropOverrides: Boolean = false) {
    modules
        .flatMap { it.getDefinitions() }
        .onEach { it.attributes["SCOPE"] = key }
        .forEach { beanRegistry.saveDefinition(it, dropOverrides) }
}

fun Component.scopedModules(owner: LifecycleOwner, key: String, vararg modules: com.ivianuu.injekt.Module, dropOverrides: Boolean = false) {
    scopedModules(key, *modules, dropOverrides = dropOverrides)
    owner.lifecycle.addObserver(GenericLifecycleObserver { source, event ->
        if (event == Lifecycle.Event.ON_DESTROY) {
            removeScope(key)
        }
    })
}

fun Component.removeScope(key: String) {
    beanRegistry.getAllDefinitions()
        .filter { it.attributes.get<String>("SCOPE") == key }
        .forEach { beanRegistry.removeDefinition(it) }
}

@Module private annotation class AppModule

@Single @AppModule
class AppDependency(val app: App)