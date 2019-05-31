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

import com.ivianuu.injekt.*
import com.ivianuu.injekt.multibinding.BindingMap
import com.ivianuu.injekt.multibinding.MapName
import com.ivianuu.injekt.multibinding.bindIntoMap
import com.ivianuu.injekt.provider.Provider

@KindRegistry([_AppService::class])
private object dummy

/**
 * @author Manuel Wrage (IVIanuu)
 */
interface AppService {
    fun start() {
    }
}

@KindAnnotation(FactoryKind::class)
@ScopeAnnotation(ApplicationScope::class)
@Interceptors([AppServiceInterceptor::class])
annotation class _AppService

object AppServices : MapName<String, AppService>

@BindingMap(AppServices::class) annotation class AppServiceMap

@Factory
class AppServiceStarter(
    @AppServiceMap private val appServices: Map<String, Provider<AppService>>
) {

}

object AppServiceInterceptor : Interceptor<AppService> {
    override fun intercept(binding: Binding<AppService>) {
        binding.bindIntoMap(AppServices, binding.type.java.name)
    }
}

@_AppService
class MyAppService : AppService {
    override fun start() {
        super.start()
    }
}