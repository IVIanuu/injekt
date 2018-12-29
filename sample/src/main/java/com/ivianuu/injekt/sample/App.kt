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
import kotlin.reflect.KClass

/**
 * @author Manuel Wrage (IVIanuu)
 */
class App : Application(), ComponentHolder {

    override val component by lazy {
        component { modules(appModule()) }
    }

    private val commands by inject<MultiBindingSet<Command>>(COMMANDS)
    private val services by inject<MultiBindingMap<KClass<out Service>, Service>>(SERVICES)

    override fun onCreate() {
        super.onCreate()
        configureInjekt {
            androidLogger()
        }

        Log.d("App", "commands ${commands.toSet()}")
        Log.d("App", "services ${services.toMap()}")
    }

}

fun App.appModule() = module {
    factory { this@appModule }
    single(createOnStart = true) { AppDependency(get()) }

    multiBindingSet<Command>(COMMANDS)
    multiBindingMap<KClass<out Service>, Service>(SERVICES)

    factory { CommandOne() } intoSet COMMANDS
    factory { CommandTwo() } intoSet COMMANDS

    factory { ServiceOne() } intoMap (SERVICES to ServiceOne::class)
    factory { ServiceTwo() } intoMap (SERVICES to ServiceTwo::class)
}

class AppDependency(val app: App)