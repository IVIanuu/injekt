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


/**
 * Returns a [Component] with convenient configurations
 */
fun <T : ContentProvider> contentProviderComponent(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Component",
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition? = null
) = component(name, createEagerInstances) {
    instance.parentComponentOrNull()?.let { components(it) }
    addInstance(instance)
    definition?.invoke(this)
}

/**
 * Returns the parent [Component] if available or null
 */
fun ContentProvider.parentComponentOrNull() =
    (context!!.applicationContext as? InjektTrait)?.component

/**
 * Returns the parent [Component] or throws
 */
fun ContentProvider.parentComponent() =
    parentComponentOrNull() ?: error("No parent found for $this")