/*
 * Copyright 2020 Manuel Wrage
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

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.ComponentOwner
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.keyOf

inline fun <reified T : Fragment> FragmentComponent(
    instance: T,
    scope: Scope = FragmentScope,
    bindingQualifier: KClass<*>? = ForFragment,
    block: ComponentBuilder.() -> Unit = {}
): Component = FragmentComponent(instance, keyOf(), scope, bindingQualifier, block)

inline fun <T : Fragment> FragmentComponent(
    instance: T,
    key: Key<T>,
    scope: Scope = FragmentScope,
    bindingQualifier: KClass<*>? = ForFragment,
    block: ComponentBuilder.() -> Unit = {}
): Component = Component {
    scopes(scope)
    instance.getClosestComponentOrNull()?.let { parents(it) }
    fragmentBindings(instance, key, bindingQualifier)
    block()
}

fun <T : Fragment> ComponentBuilder.fragmentBindings(
    instance: T,
    key: Key<T>,
    bindingQualifier: KClass<*>? = ForFragment
) {
    com.ivianuu.injekt.instance(
        instance = instance,
        key = key,
        duplicateStrategy = DuplicateStrategy.Override
    )
    com.ivianuu.injekt.alias(originalKey = key, aliasKey = keyOf<Fragment>())
    com.ivianuu.injekt.alias<Fragment>(aliasQualifier = bindingQualifier)

    maybeLifecycleBindings(instance, bindingQualifier)
    maybeViewModelStoreBindings(instance, bindingQualifier)
    maybeSavedStateBindings(instance, bindingQualifier)

    contextBindings(bindingQualifier) { instance.requireContext() }
    com.ivianuu.injekt.factory(duplicateStrategy = DuplicateStrategy.Override) { instance.childFragmentManager }
    com.ivianuu.injekt.alias<FragmentManager>(aliasQualifier = bindingQualifier)

    componentAlias(bindingQualifier)
}

annotation class FragmentScope {
    companion object : Scope
}

annotation class ChildFragmentScope {
    companion object : Scope
}

annotation class ForFragment {
    companion object : Qualifier.Element
}

annotation class ForChildFragment {
    companion object : Qualifier.Element
}

fun Fragment.getClosestComponentOrNull(): Component? {
    return getParentFragmentComponentOrNull()
        ?: getActivityComponentOrNull()
        ?: getApplicationComponentOrNull()
}

fun Fragment.getClosestComponent(): Component =
    getClosestComponentOrNull() ?: error("No close Component found for $this")

fun Fragment.getParentFragmentComponentOrNull(): Component? =
    (parentFragment as? ComponentOwner)?.component

fun Fragment.getParentFragmentComponent(): Component =
    getParentFragmentComponentOrNull() ?: error("No parent fragment Component found for $this")

fun Fragment.getActivityComponentOrNull(): Component? =
    (activity as? ComponentOwner)?.component

fun Fragment.getActivityComponent(): Component =
    getActivityComponentOrNull() ?: error("No activity Component found for $this")

fun Fragment.getApplicationComponentOrNull(): Component? =
    (activity?.application as? ComponentOwner)?.component

fun Fragment.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")
