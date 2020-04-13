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
import com.ivianuu.injekt.ApplicationScope
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.ComponentOwner
import com.ivianuu.injekt.Linker
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.android.AndroidLogger
import com.ivianuu.injekt.android.ApplicationComponent
import com.ivianuu.injekt.bind
import com.ivianuu.injekt.get
import com.ivianuu.injekt.injekt
import com.ivianuu.injekt.sample.data.Repository

val MyModule = Module(ApplicationScope) {
    bind { MyClass(get()) }
    bind(provider = object : BindingProvider<MyClass> {
        private lateinit var provider0: BindingProvider<Repository>
        override fun link(linker: Linker) {
            provider0 = linker.get()
        }

        override fun invoke(parameters: Parameters): MyClass {
            return MyClass(provider0())
        }
    })
}

class MyClass(private val repository: Repository)

class App : Application(), ComponentOwner {

    override val component by lazy {
        ApplicationComponent(this)
    }

    override fun onCreate() {
        super.onCreate()
        injekt {
            logger = AndroidLogger()
            initializeEndpoint()
        }
        get<Repository>()
    }
}
