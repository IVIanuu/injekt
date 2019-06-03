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

@Interceptors([AppServiceInterceptor::class])
annotation class AsAppService

/**
 * @author Manuel Wrage (IVIanuu)
 */
interface AppService {
    fun start() {
    }
}

@Single @ApplicationScope
class AppServiceStarter(
    private val appServices: Map<String, Provider<AppService>>
) {

    fun startServices() {
        d { "starting services $appServices" }
        appServices
            .mapValues { it.value.get() }
            .forEach { (key, service) ->
                d { "starting service for key $key -> $service" }
            }
    }

}

object AppServiceInterceptor : Interceptor<AppService> {
    override fun intercept(binding: Binding<AppService>) {
        binding.bindIntoMap<AppService, String, AppService>(binding.type.raw.java.name)
    }
}

@Single @ApplicationScope @AsAppService
class MyFirstAppService(private val app: App) : AppService {
    override fun start() {
        super.start()
    }
}

@Single @ApplicationScope @AsAppService
class MySecondAppService(@PackageName private val packageName: String) : AppService {
    override fun start() {
        super.start()
    }
}