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

import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Provider
import com.ivianuu.injekt.android.ApplicationComponent
import com.ivianuu.injekt.android.ApplicationScoped
import com.ivianuu.injekt.classOf
import com.ivianuu.injekt.composition.BindingAdapter
import com.ivianuu.injekt.composition.BindingAdapterFunction
import com.ivianuu.injekt.map
import com.ivianuu.injekt.scoped
import kotlin.reflect.KClass

interface AppService

@BindingAdapter(ApplicationComponent::class)
annotation class BindAppService

@BindingAdapterFunction(BindAppService::class)
@Module
inline fun <T : AppService> bindAppService() {
    scoped<T>()
    map<KClass<out AppService>, AppService> {
        put<T>(classOf<T>())
    }
}

@ApplicationScoped
class AppServiceRunner(
    services: Map<KClass<out AppService>, @Provider () -> AppService>
) {
    init {
        println("app service init")
        services.forEach { (key, service) ->
            println("init $key")
            service()
        }
    }
}
