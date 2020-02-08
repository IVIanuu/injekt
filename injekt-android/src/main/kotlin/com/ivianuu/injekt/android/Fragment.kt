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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.InjektTrait
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Name
import com.ivianuu.injekt.OverrideStrategy
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.Type
import com.ivianuu.injekt.typeOf

inline fun <reified T : Fragment> FragmentComponent(
    instance: T,
    scope: Any = FragmentScope,
    name: Any = ForFragment,
    block: ComponentBuilder.() -> Unit = {}
): Component = FragmentComponent(
    instance = instance,
    type = typeOf(),
    scope = scope,
    name = name,
    block = block
)

inline fun <T : Fragment> FragmentComponent(
    instance: T,
    type: Type<T>,
    scope: Any = FragmentScope,
    name: Any = ForFragment,
    block: ComponentBuilder.() -> Unit = {}
): Component =
    Component {
        scopes(scope)
        instance.getClosestComponentOrNull()?.let { dependencies(it) }
        modules(FragmentModule(instance, type, scope, name))
        block()
    }

inline fun <reified T : Fragment> FragmentModule(
    instance: T,
    scope: Any = FragmentScope,
    name: Any = ForFragment
): Module = FragmentModule(instance = instance, type = typeOf(), scope = scope, name = name)

fun <T : Fragment> FragmentModule(
    instance: T,
    type: Type<T>,
    scope: Any = FragmentScope,
    name: Any = ForFragment
) = Module {
    instance(instance = instance, type = type, overrideStrategy = OverrideStrategy.Override).apply {
        bindAlias<Fragment>()
        bindAlias<Fragment>(name)
        bindAlias<LifecycleOwner>()
        bindAlias<LifecycleOwner>(name)
        bindAlias<ViewModelStoreOwner>()
        bindAlias<ViewModelStoreOwner>(name)
        bindAlias<SavedStateRegistryOwner>()
        bindAlias<SavedStateRegistryOwner>(name)
    }

    factory(overrideStrategy = OverrideStrategy.Override) { instance.requireContext() }.bindAlias(
        name = name
    )
    factory(overrideStrategy = OverrideStrategy.Override) { instance.resources }.bindAlias(name = name)
    factory(overrideStrategy = OverrideStrategy.Override) { instance.lifecycle }.bindAlias(name = name)
    factory(overrideStrategy = OverrideStrategy.Override) { instance.viewModelStore }.bindAlias(name = name)
    factory(overrideStrategy = OverrideStrategy.Override) { instance.savedStateRegistry }.bindAlias(
        name = name
    )
    factory(overrideStrategy = OverrideStrategy.Override) { instance.childFragmentManager }.bindAlias(
        name = name
    )

    withBinding<Component>(name = scope) {
        bindAlias(name = name)
    }
}

@Scope
annotation class FragmentScope {
    companion object
}

@Scope
annotation class ChildFragmentScope {
    companion object
}

@Name
annotation class ForFragment {
    companion object
}

@Name
annotation class ForChildFragment {
    companion object
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
