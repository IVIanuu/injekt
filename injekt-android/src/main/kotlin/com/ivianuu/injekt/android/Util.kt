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
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.withBinding
import kotlinx.coroutines.CoroutineScope

@PublishedApi
internal fun ComponentBuilder.maybeLifecycleBindings(
    instance: Any,
    qualifier: Qualifier
) {
    if (instance !is LifecycleOwner) return
    instance(instance = instance, duplicateStrategy = DuplicateStrategy.Override)
        .bindAlias(qualifier = qualifier)
    factory<CoroutineScope>(duplicateStrategy = DuplicateStrategy.Override) {
        instance.lifecycleScope
    }.bindAlias(qualifier = qualifier)
    factory(duplicateStrategy = DuplicateStrategy.Override) { instance.lifecycle }
        .bindAlias(qualifier = qualifier)
}

@PublishedApi
internal fun ComponentBuilder.maybeViewModelStoreBindings(
    instance: Any,
    qualifier: Qualifier
) {
    if (instance !is ViewModelStoreOwner) return
    instance<ViewModelStoreOwner>(
        instance = instance,
        duplicateStrategy = DuplicateStrategy.Override
    )
        .bindAlias(qualifier = qualifier)
    factory(duplicateStrategy = DuplicateStrategy.Override) { instance.viewModelStore }
        .bindAlias(qualifier = qualifier)
}

@PublishedApi
internal fun ComponentBuilder.maybeSavedStateBindings(
    instance: Any,
    qualifier: Qualifier
) {
    if (instance !is SavedStateRegistryOwner) return
    instance(
        instance = instance,
        duplicateStrategy = DuplicateStrategy.Override
    ).bindAlias(qualifier = qualifier)
    factory(duplicateStrategy = DuplicateStrategy.Override) { instance.savedStateRegistry }
        .bindAlias(qualifier = qualifier)
}

@PublishedApi
internal fun ComponentBuilder.componentAlias(qualifier: Qualifier) {
    withBinding<Component> { bindAlias(qualifier = qualifier) }
}

@PublishedApi
internal fun ComponentBuilder.contextBindings(
    qualifier: Qualifier,
    definition: () -> Context
) {
    factory(duplicateStrategy = DuplicateStrategy.Override) { definition() }
        .bindAlias(qualifier = qualifier)
    resourcesBindings(qualifier = qualifier) { definition().resources!! }
}

@PublishedApi
internal fun ComponentBuilder.resourcesBindings(
    qualifier: Qualifier,
    definition: () -> Resources
) {
    factory(duplicateStrategy = DuplicateStrategy.Override) { definition() }
        .bindAlias(qualifier = qualifier)
}
