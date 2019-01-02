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
import com.ivianuu.injekt.*
import com.ivianuu.injekt.common.instanceModule

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : ContentProvider> contentProviderComponent(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Component",
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition? = null
) = component(name, createEagerInstances) {
    dependencies(contentProviderDependencies(instance))
    modules(instanceModule(instance), contentProviderModule(instance))
    definition?.invoke(this)
}

/**
 * Returns dependencies for [instance]
 */
fun contentProviderDependencies(instance: ContentProvider): Set<Component> {
    val dependencies = mutableSetOf<Component>()
    (instance.context!!.applicationContext as? InjektTrait)?.component?.let {
        dependencies.add(
            it
        )
    }
    return dependencies
}

const val CONTENT_PROVIDER = "content_provider"
const val CONTENT_PROVIDER_CONTEXT = "content_provider_context"

/**
 * Returns a [Module] with convenient declarations
 */
fun <T : ContentProvider> contentProviderModule(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Module"
) = module(name) {
    // service
    factory(CONTENT_PROVIDER) { instance as ContentProvider }
    factory(CONTENT_PROVIDER_CONTEXT) { instance.context!! }
}