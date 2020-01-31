/*
 * Copyright 2019 Manuel Wrage
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
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.typeOf

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

fun <T : Fragment> T.FragmentComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    Component {
        scopes(FragmentScope)
        getClosestComponentOrNull()?.let { dependencies(it) }
        modules(FragmentModule())
        block?.invoke(this)
    }

fun <T : Fragment> T.ChildFragmentComponent(block: (ComponentBuilder.() -> Unit)? = null): Component =
    Component {
        scopes(ChildFragmentScope)
        getClosestComponentOrNull()?.let { dependencies(it) }
        modules(ChildFragmentModule())
        block?.invoke(this)
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

fun <T : Fragment> T.FragmentModule(): Module = Module {
    include(InternalFragmentModule(scope = FragmentScope, name = ForFragment))
}

fun <T : Fragment> T.ChildFragmentModule(): Module = Module {
    include(InternalFragmentModule(scope = ChildFragmentScope, name = ForChildFragment))
}

private fun <T : Fragment> T.InternalFragmentModule(
    scope: Any,
    name: Any
) = Module {
    instance(instance = this@InternalFragmentModule, type = typeOf(this@InternalFragmentModule), override = true).apply {
        bindAlias<Fragment>()
        bindAlias<Fragment>(name)
        bindAlias<LifecycleOwner>()
        bindAlias<LifecycleOwner>(name)
        bindAlias<ViewModelStoreOwner>()
        bindAlias<ViewModelStoreOwner>(name)
        bindAlias<SavedStateRegistryOwner>()
        bindAlias<SavedStateRegistryOwner>(name)
    }

    factory(override = true) { requireContext() }.bindAlias(name = name)
    factory(override = true) { resources }.bindAlias(name = name)
    factory(override = true) { lifecycle }.bindAlias(name = name)
    factory(override = true) { viewModelStore }.bindAlias(name = name)
    factory(override = true) { savedStateRegistry }.bindAlias(name = name)
    factory(override = true) { childFragmentManager }.bindAlias(name = name)

    withBinding<Component>(name = scope) {
        bindAlias(name = name)
    }
}
