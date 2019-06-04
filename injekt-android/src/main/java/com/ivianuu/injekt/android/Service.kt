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

package com.ivianuu.injekt.android

import android.app.Service
import android.content.Context
import com.ivianuu.injekt.*
import com.ivianuu.injekt.constant.constant

@ScopeAnnotation(ServiceScope.Companion::class)
annotation class ServiceScope {
    companion object : NamedScope("ServiceScope")
}

@Name(ForService.Companion::class)
annotation class ForService {
    companion object : Qualifier
}

fun <T : Service> T.serviceComponent(
    scope: Scope? = ServiceScope,
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component = androidComponent(
    scope, modules, dependencies,
    { serviceModule() },
    { getClosestComponentOrNull() }
)

fun Service.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

fun Service.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close component found for $this")

fun Service.getApplicationComponentOrNull(): Component? = (application as? InjektTrait)?.component

fun Service.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")

fun <T : Service> T.serviceModule(): Module = module {
    constant(this@serviceModule, override = true).apply {
        bindType<Service>()
        bindAlias<Context>(ForService)
        bindType<Context>()
    }

    factory(override = true) { resources } bindName ForService
}