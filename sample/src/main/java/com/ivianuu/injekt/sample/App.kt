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
import com.ivianuu.injekt.annotations.Single
import kotlin.reflect.KClass

/**
 * @author Manuel Wrage (IVIanuu)
 */
class App : Application(), ComponentHolder {

    override val component by lazy {
        component { modules(appModule()) }
    }

    private val servicesMap by inject<Map<KClass<out Service>, Service>>(SERVICES_MAP)
    private val servicesSet by inject<Set<Service>>(SERVICES_SET)

    override fun onCreate() {
        super.onCreate()
        configureInjekt {
            androidLogger()
        }

        Log.d("App", "services set $servicesSet \n\n services map $servicesMap")
    }

}

const val SERVICES_SET = "servicesSet"
const val SERVICES_MAP = "servicesMap"

fun App.appModule() = module {
    factory { this@appModule }
    factory { MyServiceOne() } intoSet SERVICES_SET intoMap (SERVICES_MAP to MyServiceOne::class)
    factory { MyServiceTwo() } intoSet SERVICES_SET intoMap (SERVICES_MAP to MyServiceTwo::class)
}

@Single(createOnStart = true)
class AppDependency(val app: App)