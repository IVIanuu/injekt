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
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDefinition
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.addInstance
import com.ivianuu.injekt.component
import com.ivianuu.injekt.dependencies
import com.ivianuu.injekt.scopeNames

const val SERVICE_SCOPE = "service_scope"

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <T : Service> T.serviceComponent(
    name: String? = javaClass.simpleName + "Component",
    deferCreateEagerInstances: Boolean = false,
    definition: ComponentDefinition = {}
): Component = component(name, deferCreateEagerInstances) {
    scopeNames(SERVICE_SCOPE)
    getApplicationComponentOrNull()?.let { dependencies(it) }
    addInstance(this@serviceComponent)
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
    getApplicationComponentOrNull() ?: error("No application found for $this")