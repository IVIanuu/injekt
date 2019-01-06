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

package com.ivianuu.injekt.android.fragment

import androidx.fragment.app.Fragment
import com.ivianuu.injekt.*


/**
 * Returns a [Component] with convenient configurations
 */
fun <T : Fragment> fragmentComponent(
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
fun Fragment.parentComponentOrNull(): Component? {
    var parentFragment = parentFragment

    while (parentFragment != null) {
        if (parentFragment is InjektTrait) {
            return parentFragment.component
        }
        parentFragment = parentFragment.parentFragment
    }

    (activity as? InjektTrait)?.component?.let { return it }
    (activity?.applicationContext as? InjektTrait)?.component?.let { return it }

    return null
}

/**
 * Returns the parent [Component] or throws
 */
fun Fragment.parentComponent() = parentComponentOrNull() ?: error("No parent found for $this")