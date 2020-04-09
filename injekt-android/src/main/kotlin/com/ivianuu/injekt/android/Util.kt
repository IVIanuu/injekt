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
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import kotlinx.coroutines.CoroutineScope
import kotlin.reflect.KClass

internal fun Key<*>.isSubTypeOf(classifier: KClass<*>): Boolean =
    this.classifier.java.isAssignableFrom(classifier.java)

@PublishedApi
internal fun ComponentBuilder.maybeLifecycleBindings(
    instance: Any,
    qualifier: Qualifier
) {
    if (instance !is LifecycleOwner) return
    instance(instance = instance, duplicateStrategy = DuplicateStrategy.Override)
    alias<LifecycleOwner>(aliasQualifier = qualifier)
    factory<CoroutineScope>(duplicateStrategy = DuplicateStrategy.Override) { instance.lifecycleScope }
    alias<CoroutineScope>(aliasQualifier = qualifier)
    factory(duplicateStrategy = DuplicateStrategy.Override) { instance.lifecycle }
    alias<Lifecycle>(aliasQualifier = qualifier)
}

@PublishedApi
internal fun ComponentBuilder.maybeViewModelStoreBindings(
    instance: Any,
    qualifier: Qualifier
) {
    if (instance !is ViewModelStoreOwner) return
    instance(
        instance = instance,
        duplicateStrategy = DuplicateStrategy.Override
    )
    alias<ViewModelStoreOwner>(aliasQualifier = qualifier)
    factory(duplicateStrategy = DuplicateStrategy.Override) { instance.viewModelStore }
    alias<ViewModelStore>(aliasQualifier = qualifier)
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
    )
    alias<SavedStateRegistryOwner>(aliasQualifier = qualifier)
    factory(duplicateStrategy = DuplicateStrategy.Override) { instance.savedStateRegistry }
    alias<SavedStateRegistry>(aliasQualifier = qualifier)
}

@PublishedApi
internal fun ComponentBuilder.componentAlias(qualifier: Qualifier) {
    alias<Component>(aliasQualifier = qualifier)
}

@PublishedApi
internal fun ComponentBuilder.contextBindings(
    qualifier: Qualifier,
    definition: () -> Context
) {
    factory(duplicateStrategy = DuplicateStrategy.Override) { definition() }
    alias<Context>(aliasQualifier = qualifier)
    resourcesBindings(qualifier = qualifier) { definition().resources!! }
}

@PublishedApi
internal fun ComponentBuilder.resourcesBindings(
    qualifier: Qualifier,
    definition: () -> Resources
) {
    factory(duplicateStrategy = DuplicateStrategy.Override) { definition() }
    alias<Resources>(aliasQualifier = qualifier)
}
