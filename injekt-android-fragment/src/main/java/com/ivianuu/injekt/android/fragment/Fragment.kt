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
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDefinition
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.StringQualifier
import com.ivianuu.injekt.StringScope
import com.ivianuu.injekt.common.addInstance
import com.ivianuu.injekt.component
import com.ivianuu.injekt.dependencies
import com.ivianuu.injekt.scopes

/**
 * Fragment scope
 */
object FragmentScope : StringScope("FragmentScope")

/**
 * Child fragment scope
 */
object ChildFragmentScope : StringScope("ChildFragmentScope")

/**
 * Fragment qualifier
 */
object ForFragment : StringQualifier("ForFragment")

/**
 * Child fragment qualifier
 */
object ForChildFragment : StringQualifier("ForChildFragment")

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <reified T : Fragment> T.fragmentComponent(
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition = {}
): Component = component(createEagerInstances) {
    scopes(FragmentScope)
    (getParentFragmentComponentOrNull()
        ?: getActivityComponentOrNull()
        ?: getApplicationComponentOrNull())?.let(this::dependencies)
    addInstance(this@fragmentComponent)
    definition.invoke(this)
}

/**
 * Returns a [Component] with convenient configurations
 */
inline fun <reified T : Fragment> T.childFragmentComponent(
    createEagerInstances: Boolean = true,
    definition: ComponentDefinition = {}
): Component = component(createEagerInstances) {
    scopes(ChildFragmentScope)
    (getParentFragmentComponentOrNull()
        ?: getActivityComponentOrNull()
        ?: getApplicationComponentOrNull())?.let(this::dependencies)
    addInstance(this@childFragmentComponent)
    definition.invoke(this)
}

/**
 * Returns the [Component] of the parent fragment or null
 */
fun Fragment.getParentFragmentComponentOrNull(): Component? =
    (parentFragment as? InjektTrait)?.component

/**
 * Returns the [Component] of the parent fragment or throws
 */
fun Fragment.getParentFragmentComponent(): Component =
    getParentFragmentComponentOrNull() ?: error("No parent fragment component found for $this")

/**
 * Returns the [Component] of the activity or null
 */
fun Fragment.getActivityComponentOrNull(): Component? =
    (activity as? InjektTrait)?.component

/**
 * Returns the [Component] of the activity or throws
 */
fun Fragment.getActivityComponent(): Component =
    getActivityComponentOrNull() ?: error("No activity component found for $this")

/**
 * Returns the [Component] of the application or null
 */
fun Fragment.getApplicationComponentOrNull(): Component? =
    (activity?.application as? InjektTrait)?.component

/**
 * Returns the [Component] of the application or throws
 */
fun Fragment.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application component found for $this")