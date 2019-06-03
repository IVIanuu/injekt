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

/**
 * Service scope
 */
@ScopeAnnotation(ServiceScope.Companion::class)
annotation class ServiceScope {
    companion object : Scope
}

/**
 * Service name
 */
@Name(ForService.Companion::class)
annotation class ForService {
    companion object : Qualifier
}

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : Service> T.serviceComponent(
    scope: Scope? = ServiceScope,
    modules: Iterable<Module> = emptyList(),
    dependencies: Iterable<Component> = emptyList()
): Component = androidComponent(
    scope, modules, dependencies,
    { serviceModule() },
    { getClosestComponentOrNull() }
)

/**
 * Returns the closest [Component] or null
 */
fun Service.getClosestComponentOrNull(): Component? =
    getApplicationComponentOrNull()

/**
 * Returns the closest [Component]
 */
fun Service.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close component found for $this")

/**
 * Returns the parent [Component] if available or null
 */
fun Service.getApplicationComponentOrNull(): Component? = (application as? InjektTrait)?.component

/**
 * Returns the parent [Component] or throws
 */
fun Service.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")

/**
 * Returns a [Module] with convenient bindings
 */
fun <T : Service> T.serviceModule(): Module = module {
    constant(this@serviceModule, override = true).apply {
        bindType<Service>()
        bindAlias<Context>(ForService)
        bindType<Context>()
    }

    factory(override = true) { resources } bindName ForService
}