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
import com.ivianuu.injekt.*
import com.ivianuu.injekt.android.androidLogger

/**
 * @author Manuel Wrage (IVIanuu)
 */
class App : Application(), ComponentHolder {

    override val component by lazy {
        component(listOf(appModule(this)))
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            InjektPlugins.logger = androidLogger()
        }
    }

}

fun appModule(app: App) = module {
    factory { app }
    single(createOnStart = true) { AppDependency(get()) }
}

class AppDependency(val app: App)