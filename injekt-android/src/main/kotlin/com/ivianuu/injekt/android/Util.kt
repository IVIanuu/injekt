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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryOwner
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import kotlinx.coroutines.CoroutineScope

@PublishedApi
internal fun ComponentBuilder.maybeLifecycleBindings(
    instance: Any,
    qualifier: KClass<*>?
) {
    if (instance !is LifecycleOwner) return
    com.ivianuu.injekt.instance(
        instance = instance,
        duplicateStrategy = DuplicateStrategy.Override
    )
    com.ivianuu.injekt.alias<LifecycleOwner>(aliasQualifier = qualifier)
    com.ivianuu.injekt.factory<CoroutineScope>(duplicateStrategy = DuplicateStrategy.Override) { instance.lifecycleScope }
    com.ivianuu.injekt.alias<CoroutineScope>(aliasQualifier = qualifier)
    com.ivianuu.injekt.factory(duplicateStrategy = DuplicateStrategy.Override) { instance.lifecycle }
    com.ivianuu.injekt.alias<Lifecycle>(aliasQualifier = qualifier)
}

@PublishedApi
internal fun ComponentBuilder.maybeViewModelStoreBindings(
    instance: Any,
    qualifier: KClass<*>?
) {
    if (instance !is ViewModelStoreOwner) return
    com.ivianuu.injekt.instance(
        instance = instance,
        duplicateStrategy = DuplicateStrategy.Override
    )
    com.ivianuu.injekt.alias<ViewModelStoreOwner>(aliasQualifier = qualifier)
    com.ivianuu.injekt.factory(duplicateStrategy = DuplicateStrategy.Override) { instance.viewModelStore }
    com.ivianuu.injekt.alias<ViewModelStore>(aliasQualifier = qualifier)
}

@PublishedApi
internal fun ComponentBuilder.maybeSavedStateBindings(
    instance: Any,
    qualifier: KClass<*>?
) {
    if (instance !is SavedStateRegistryOwner) return
    com.ivianuu.injekt.instance(
        instance = instance,
        duplicateStrategy = DuplicateStrategy.Override
    )
    com.ivianuu.injekt.alias<SavedStateRegistryOwner>(aliasQualifier = qualifier)
    com.ivianuu.injekt.factory(duplicateStrategy = DuplicateStrategy.Override) { instance.savedStateRegistry }
    com.ivianuu.injekt.alias<SavedStateRegistry>(aliasQualifier = qualifier)
}

@PublishedApi
internal fun ComponentBuilder.componentAlias(qualifier: KClass<*>?) {
    com.ivianuu.injekt.alias<Component>(aliasQualifier = qualifier)
}

@PublishedApi
internal fun ComponentBuilder.contextBindings(
    qualifier: KClass<*>?,
    definition: () -> Context
) {
    com.ivianuu.injekt.factory(duplicateStrategy = DuplicateStrategy.Override) { definition() }
    com.ivianuu.injekt.alias<Context>(aliasQualifier = qualifier)
    resourcesBindings(qualifier = qualifier) { definition().resources!! }
}

@PublishedApi
internal fun ComponentBuilder.resourcesBindings(
    qualifier: KClass<*>?,
    definition: () -> Resources
) {
    com.ivianuu.injekt.factory(duplicateStrategy = DuplicateStrategy.Override) { definition() }
    com.ivianuu.injekt.alias<Resources>(aliasQualifier = qualifier)
}
