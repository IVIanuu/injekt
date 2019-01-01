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
import android.util.Log
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.androidLogger
import com.ivianuu.injekt.codegen.Name
import com.ivianuu.injekt.codegen.Param
import com.ivianuu.injekt.codegen.Single
import com.ivianuu.injekt.sample.multibinding.MultiBindingMap
import com.ivianuu.injekt.sample.multibinding.MultiBindingSet
import com.ivianuu.injekt.sample.multibinding.bindIntoMap
import com.ivianuu.injekt.sample.multibinding.bindIntoSet
import kotlin.reflect.KClass

/**
 * @author Manuel Wrage (IVIanuu)
 */
class App : Application(), ComponentHolder {

    override val component by lazy {
        component { modules(autoAppModule, appModule()) }
    }

    private val servicesMap by inject<MultiBindingMap<KClass<out Service>, Service>>(SERVICES_MAP)
    private val servicesSet by inject<MultiBindingSet<Service>>(SERVICES_SET)

    override fun onCreate() {
        super.onCreate()
        configureInjekt {
            androidLogger()
        }

        Log.d("App", "services set $servicesSet \n\n services map $servicesMap")

        component.declarationRegistry.getAllDeclarations()
            .filter { it.type == MyServiceOne::class }
            .forEach {
                Log.d(
                    "testt",
                    "module for $it is ${it.module}, services module $servicesModule"
                )
            }
    }

}

const val SERVICES_SET = "servicesSet"
const val SERVICES_MAP = "servicesMap"

fun App.appModule() = module {
    factory { this@appModule }
    module(servicesModule)
}

private val servicesModule = module {
    module(autoServicesModule)
    bindIntoSet<Service, MyServiceOne>(SERVICES_SET)
    bindIntoMap<KClass<out Service>, Service, MyServiceOne>(SERVICES_MAP, MyServiceOne::class)

    bindIntoSet<Service, MyServiceTwo>(SERVICES_SET)
    bindIntoMap<KClass<out Service>, Service, MyServiceTwo>(SERVICES_MAP, MyServiceTwo::class)
}

@Single
@AutoAppModule
class AppDependency(
    val app: App,
    @Param val appLazy: Lazy<App>,
    @Name("namedd") val appProvider: Provider<App>,
    @Param val paramete: String,
    @Param val parametew: String,
    @Name("named") val named: String,
    @Param val parf: String
)