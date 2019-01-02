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
import com.ivianuu.injekt.common.instanceModule

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : Service> serviceComponent(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Component",
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition? = null
) = component(name, createEagerInstances) {
    (instance.application as? ComponentHolder)?.component?.let {
        if (!componentRegistry.dependsOn(it)) dependencies(it)
    }

    modules(instanceModule(instance), serviceModule(instance))

    definition?.invoke(this)
}

const val SERVICE = "service"
const val SERVICE_CONTEXT = "service_context"

/**
 * Returns a [Module] with convenient declarations
 */
fun <T : Service> serviceModule(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Module"
) = module(name) {
    // service
    factory(SERVICE) { instance as Service }
    bind<Context, Service>(SERVICE_CONTEXT)
}

fun ComponentContext.service() = get<Service>(SERVICE)
fun ComponentContext.serviceContext() = get<Context>(SERVICE_CONTEXT)