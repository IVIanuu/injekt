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

/**
 * Returns a [Component] with convenient configurations
 */
fun <T : Fragment> fragmentComponent(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Component",
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition? = null
) = component(name, createEagerInstances) {
    fragmentDependencies(instance)
        .filterNot { componentRegistry.dependsOn(it) }
        .forEach { dependencies(it) }
    modules(instanceModule(instance), fragmentModule(instance))
    definition?.invoke(this)
}

/**
 * Returns dependencies for [instance]
 */
fun fragmentDependencies(instance: Fragment): Set<Component> {
    val dependencies = mutableSetOf<Component>()

    var parentFragment = instance.parentFragment
    while (parentFragment != null) {
        (parentFragment as? ComponentHolder)?.component?.let { dependencies.add(it) }
        parentFragment = parentFragment.parentFragment
    }

    (instance.activity as? ComponentHolder)?.component?.let { dependencies.add(it) }

    (instance.activity?.applicationContext as? ComponentHolder)?.component?.let {
        dependencies.add(
            it
        )
    }

    return dependencies
}

/**
 * Returns a [Module] with convenient declarations
 */
fun <T : Fragment> fragmentModule(
    instance: T,
    name: String? = instance.javaClass.simpleName + "Module"
) = module(name) {

}