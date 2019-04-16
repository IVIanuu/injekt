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

/**
 * Service name
 */
object ForService : StringName("ForService")

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <T : Service> T.serviceComponent(
    definition: Component.() -> Unit = {}
): Component = component {
    modules(serviceModule())
    getApplicationComponentOrNull()?.let { dependencies(it) }
    definition.invoke(this)
}

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
    add(
        Binding(
            type = this@serviceModule::class,
            kind = Binding.Kind.SINGLE,
            definition = { this@serviceModule }
        )
    ) bindType Service::class

    single<Context>(ForService) { this@serviceModule }
}