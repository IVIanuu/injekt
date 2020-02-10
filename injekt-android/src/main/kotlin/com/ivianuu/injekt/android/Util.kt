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

import android.content.Context
import android.content.res.Resources
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ModuleBuilder
import com.ivianuu.injekt.OverrideStrategy
import kotlinx.coroutines.CoroutineScope

internal fun ModuleBuilder.maybeLifecycleBindings(
    instance: Any,
    name: Any
) {
    if (instance !is LifecycleOwner) return
    instance<LifecycleOwner>(instance = instance, overrideStrategy = OverrideStrategy.Override)
        .bindAlias(name = name)
    factory<CoroutineScope>(overrideStrategy = OverrideStrategy.Override) {
        instance.lifecycleScope
    }.bindAlias(name = name)
    factory(overrideStrategy = OverrideStrategy.Override) { instance.lifecycle }
        .bindAlias(name = name)
}

internal fun ModuleBuilder.maybeViewModelStoreBindings(
    instance: Any,
    name: Any
) {
    if (instance !is ViewModelStoreOwner) return
    instance<ViewModelStoreOwner>(instance = instance, overrideStrategy = OverrideStrategy.Override)
        .bindAlias(name = ForActivity)
    factory(overrideStrategy = OverrideStrategy.Override) { instance.viewModelStore }
        .bindAlias(name = name)
}

internal fun ModuleBuilder.maybeSavedStateBindings(
    instance: Any,
    name: Any
) {
    if (instance !is SavedStateRegistryOwner) return
    instance<SavedStateRegistryOwner>(
        instance = instance,
        overrideStrategy = OverrideStrategy.Override
    )
        .bindAlias(name = ForActivity)
    factory(overrideStrategy = OverrideStrategy.Override) { instance.savedStateRegistry }
        .bindAlias(name = name)
}

internal fun ModuleBuilder.componentAlias(scope: Any) {
    withBinding<Component> { bindAlias(name = scope) }
}

internal fun ModuleBuilder.contextBindings(
    name: Any,
    definition: () -> Context
) {
    factory(overrideStrategy = OverrideStrategy.Override) { definition() }
        .bindAlias(name = name)
    resourcesBindings(name) { definition().resources!! }
}

internal fun ModuleBuilder.resourcesBindings(
    name: Any,
    definition: () -> Resources
) {
    factory(overrideStrategy = OverrideStrategy.Override) { definition() }
        .bindAlias(name = name)
}

