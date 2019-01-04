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
import com.ivianuu.injekt.common.instanceModule

const val FRAGMENT_SCOPE = "fragment_scope"

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : Fragment> fragmentComponent(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Component",
    scope: String? = FRAGMENT_SCOPE,
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition? = null
) = component(name, scope, createEagerInstances) {
    instance.parentComponent()?.let { components(it) }
    modules(instanceModule(instance), fragmentModule(instance))
    definition?.invoke(this)
}

/**
 * Returns the parent [Component] if available or null
 */
fun Fragment.parentComponent(): Component? {
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
 * Returns a [Module] with convenient definitions
 */
fun <T : Fragment> fragmentModule(
    instance: T,
    name: String? = "FragmentModule"
) = module(name) {

}