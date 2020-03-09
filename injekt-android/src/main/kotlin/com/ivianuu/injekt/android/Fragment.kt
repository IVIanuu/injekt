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
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.QualifierMarker
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.ScopeMarker
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.keyOf

inline fun <reified T : Fragment> FragmentComponent(
    instance: T,
    scope: Scope = FragmentScope,
    qualifier: Qualifier = ForFragment,
    block: ComponentBuilder.() -> Unit = {}
): Component = FragmentComponent(
    instance = instance,
    key = keyOf(),
    scope = scope,
    qualifier = qualifier,
    block = block
)

inline fun <T : Fragment> FragmentComponent(
    instance: T,
    key: Key<T>,
    scope: Scope = FragmentScope,
    qualifier: Qualifier = ForFragment,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    Component {
        scopes(scope)
        instance.getClosestComponentOrNull()?.let { dependencies(it) }
        fragmentBindings(instance, key, qualifier)
        block()
    }

fun <T : Fragment> ComponentBuilder.fragmentBindings(
    instance: T,
    key: Key<T>,
    qualifier: Qualifier = ForFragment
) {
    instance(instance = instance, key = key, duplicateStrategy = DuplicateStrategy.Override)
    alias(originalKey = key, aliasKey = keyOf<Fragment>())
    alias<Fragment>(aliasQualifier = qualifier)

    maybeLifecycleBindings(instance, qualifier)
    maybeViewModelStoreBindings(instance, qualifier)
    maybeSavedStateBindings(instance, qualifier)

    contextBindings(qualifier) { instance.requireContext() }
    factory(duplicateStrategy = DuplicateStrategy.Override) { instance.childFragmentManager }
    alias<FragmentManager>(aliasQualifier = qualifier)

    componentAlias(qualifier)
}

@ScopeMarker
annotation class FragmentScope {
    companion object : Scope
}

@ScopeMarker
annotation class ChildFragmentScope {
    companion object : Scope
}

@QualifierMarker
annotation class ForFragment {
    companion object : Qualifier.Element
}

@QualifierMarker
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
    (parentFragment as? InjektTrait)?.component

fun Fragment.getParentFragmentComponent(): Component =
    getParentFragmentComponentOrNull() ?: error("No parent fragment Component found for $this")

fun Fragment.getActivityComponentOrNull(): Component? =
    (activity as? InjektTrait)?.component

fun Fragment.getActivityComponent(): Component =
    getActivityComponentOrNull() ?: error("No activity Component found for $this")

fun Fragment.getApplicationComponentOrNull(): Component? =
    (activity?.application as? InjektTrait)?.component

fun Fragment.getApplicationComponent(): Component =
    getApplicationComponentOrNull() ?: error("No application Component found for $this")
