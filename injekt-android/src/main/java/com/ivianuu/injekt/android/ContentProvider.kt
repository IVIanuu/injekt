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

import android.content.ContentProvider
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDefinition
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.addInstance
import com.ivianuu.injekt.component
import com.ivianuu.injekt.dependencies
import com.ivianuu.injekt.scopeNames

const val CONTENT_PROVIDER_SCOPE = "content_provider_scope"

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : ContentProvider> contentProviderComponent(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Component",
    deferCreateEagerInstances: Boolean = false,
    definition: ComponentDefinition? = null
): Component = component(name, deferCreateEagerInstances) {
    scopeNames(CONTENT_PROVIDER_SCOPE)
    instance.getApplicationComponentOrNull()?.let { dependencies(it) }
    addInstance(instance)
    definition?.invoke(this)
}

/**
 * Returns the parent [Component] if available or null
 */
fun ContentProvider.getApplicationComponentOrNull(): Component? =
    (context?.applicationContext as? InjektTrait)?.component

/**
 * Returns the parent [Component] or throws
 */
fun ContentProvider.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application found for $this")