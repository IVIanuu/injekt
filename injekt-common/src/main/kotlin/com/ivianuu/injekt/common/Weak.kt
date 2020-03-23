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

package com.ivianuu.injekt.common

import com.ivianuu.injekt.Behavior
import com.ivianuu.injekt.Binding
import com.ivianuu.injekt.BindingProvider
import com.ivianuu.injekt.BoundBehavior
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentBuilder
import com.ivianuu.injekt.DelegatingBindingProvider
import com.ivianuu.injekt.DuplicateStrategy
import com.ivianuu.injekt.IntoComponent
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Parameters
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Single
import com.ivianuu.injekt.Tag
import com.ivianuu.injekt.TagMarker
import com.ivianuu.injekt.keyOf
import java.lang.ref.WeakReference

/**
 * Holds instances in a [WeakReference]
 */
object WeakBehavior : Behavior.Element {
    override fun <T> apply(provider: BindingProvider<T>): BindingProvider<T> =
        WeakProvider(provider)
}

inline fun <reified T> ComponentBuilder.weak(
    qualifier: Qualifier = Qualifier.None,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    noinline provider: BindingProvider<T>
) = weak(
    key = keyOf(qualifier = qualifier),
    behavior = behavior,
    duplicateStrategy = duplicateStrategy,
    provider = provider
)

/**
 * Dsl builder for [WeakBehavior] + [BoundBehavior]
 */
fun <T> ComponentBuilder.weak(
    key: Key<T>,
    behavior: Behavior = Behavior.None,
    duplicateStrategy: DuplicateStrategy = DuplicateStrategy.Fail,
    provider: BindingProvider<T>
) = bind(
    Binding(
        key = key,
        behavior = WeakBehavior + BoundBehavior() + behavior,
        duplicateStrategy = duplicateStrategy,
        provider = provider
    )
)

/**
 * Annotation for the [WeakBehavior]
 */
@TagMarker
annotation class Weak {
    companion object : Tag
}

@IntoComponent(invokeOnInit = true)
private fun ComponentBuilder.weakBindingInterceptor() {
    bindingInterceptor { binding ->
        if (Single in binding.tags) {
            binding.copy(behavior = binding.behavior + WeakBehavior)
        } else {
            binding
        }
    }
}

private class WeakProvider<T>(delegate: BindingProvider<T>) :
    DelegatingBindingProvider<T>(delegate) {

    private var ref: WeakReference<Wrapper<T>>? = null

    override fun invoke(component: Component, parameters: Parameters): T {
        var valueWrapper = ref?.get()
        if (valueWrapper == null) {
            valueWrapper = Wrapper(super.invoke(component, parameters))
            ref = WeakReference(valueWrapper)
        }

        return valueWrapper.value
    }

    /**
     * We need the wrapper because [T] might be nullable
     */
    private class Wrapper<T>(val value: T)
}
